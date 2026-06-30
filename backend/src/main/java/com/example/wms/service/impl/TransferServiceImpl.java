package com.example.wms.service.impl;

import com.example.wms.dto.transfer.*;
import com.example.wms.entity.*;
import com.example.wms.repository.*;
import com.example.wms.service.TransferService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 转包服务实现
 *
 * 支持两种转包模式：
 * 1. 拆包：targetKanbanNo为空 或 指定的看板号不存在 → 创建新看板
 * 2. 合包：指定的看板号已存在 → 累加到已有看板
 *
 * 并发保护：源看板使用悲观锁 + 原子扣减，防止超转包
 */
@Service
public class TransferServiceImpl implements TransferService {

    private static final DateTimeFormatter DOC_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger DOC_NO_SEQUENCE = new AtomicInteger(0);

    @PersistenceContext
    private EntityManager entityManager;

    private final InboundKanbanLabelRepository kanbanLabelRepository;
    private final PackageTransferRepository transferRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderDetailRepository outboundOrderDetailRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderDetailRepository inboundOrderDetailRepository;
    private final OutboundHistoryRepository outboundHistoryRepository;

    public TransferServiceImpl(InboundKanbanLabelRepository kanbanLabelRepository,
                               PackageTransferRepository transferRepository,
                               InventoryStockRepository inventoryStockRepository,
                               OutboundOrderRepository outboundOrderRepository,
                               OutboundOrderDetailRepository outboundOrderDetailRepository,
                               InboundOrderRepository inboundOrderRepository,
                               InboundOrderDetailRepository inboundOrderDetailRepository,
                               OutboundHistoryRepository outboundHistoryRepository) {
        this.kanbanLabelRepository = kanbanLabelRepository;
        this.transferRepository = transferRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.outboundOrderRepository = outboundOrderRepository;
        this.outboundOrderDetailRepository = outboundOrderDetailRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.inboundOrderDetailRepository = inboundOrderDetailRepository;
        this.outboundHistoryRepository = outboundHistoryRepository;
    }

    @Override
    @Transactional
    public TransferResultDTO executeTransfer(TransferRequest request, String operator) {
        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);

        // ==================== 1. 查询源看板（带悲观锁）====================
        InboundKanbanLabel source = kanbanLabelRepository.findByKanbanNoWithLock(request.getSourceKanbanNo())
                .orElseThrow(() -> new EntityNotFoundException("看板号不存在: " + request.getSourceKanbanNo()));

        // ==================== 2. 源看板基础校验 ====================
        if (!"已入库".equals(source.getLabelStatus())) {
            throw new IllegalStateException("只有已入库的看板才能转包，当前状态: " + source.getLabelStatus());
        }
        if (Boolean.TRUE.equals(source.getSealed())) {
            throw new IllegalStateException("该看板已封存，无法转包，请先解封");
        }
        if ("已转包".equals(source.getTransferStatus())) {
            throw new IllegalStateException("该看板已全部转包，不能继续操作");
        }
        if ("已出库".equals(source.getTransferStatus())) {
            throw new IllegalStateException("该看板已出库，不能进行转包");
        }

        int transferQty = request.getTransferQty();

        if (transferQty <= 0) {
            throw new IllegalArgumentException("转移数量必须大于0");
        }

        // labelQty 表示当前剩余数量，转包时已经实时扣减；这里只扣除冻结量。
        int availableQty = calculateAvailableQty(source);

        if (transferQty > availableQty) {
            throw new IllegalArgumentException(
                    "转移数量(" + transferQty + ")超过源看板可用数量(" + availableQty + ")");
        }

        // ==================== 3. 判定转包模式 ====================
        String targetKanbanNo = request.getTargetKanbanNo();
        boolean userSpecified = targetKanbanNo != null && !targetKanbanNo.trim().isEmpty();
        boolean isMergeMode = false;
        InboundKanbanLabel targetLabel = null;
        String inboundDocNo = null;

        if (userSpecified) {
            targetLabel = kanbanLabelRepository.findByKanbanNo(targetKanbanNo).orElse(null);
            if (targetLabel != null) {
                // 目标看板存在 → 合包模式
                isMergeMode = true;
            }
            // 否则目标看板不存在 → 拆包模式，用户指定的看板号会被创建
        }

        // ==================== 4. 合包模式校验 ====================
        if (isMergeMode) {
            validateMergeTarget(source, targetLabel);
            inboundDocNo = targetLabel.getDocNo(); // 合包复用原入库单
        }

        // ==================== 5. 执行库存扣减（原子操作）====================
        // 使用原子扣减，防止并发超转包
        int updated = kanbanLabelRepository.decreaseLabelQty(source.getKanbanNo(), transferQty);
        if (updated == 0) {
            throw new IllegalStateException("源看板数量已变化，请重试");
        }
        // 将@Modifying查询的变更刷入数据库，并清除第一层缓存
        // 确保后续findByKanbanNo能读到最新数据
        entityManager.flush();
        entityManager.clear();

        // 扣减后在Java代码中精确设置转包状态
        InboundKanbanLabel freshSource = kanbanLabelRepository.findByKanbanNo(source.getKanbanNo()).orElse(source);
        if (freshSource.getLabelQty() != null && freshSource.getLabelQty() <= 0) {
            freshSource.setTransferStatus("已转包");
        } else {
            freshSource.setTransferStatus("部分转包");
        }
        kanbanLabelRepository.save(freshSource);

        // ==================== 6. 创建出库单 ====================
        String outboundDocNo = generateOutboundDocNo();
        OutboundOrder outboundOrder = createOutboundOrder(source, transferQty, outboundDocNo, isMergeMode, targetKanbanNo, currentOperator, now);
        outboundOrder = outboundOrderRepository.save(outboundOrder); // 保存后获得真实ID

        OutboundOrderDetail outboundDetail = createOutboundDetail(source, transferQty, outboundDocNo, isMergeMode, targetKanbanNo, currentOperator, now);
        outboundDetail.setOutboundOrderId(outboundOrder.getId()); // 设置真实FK
        outboundOrderDetailRepository.save(outboundDetail);

        // ==================== 7. 扣减库存 ====================
        String stockArea = getStockArea(source);
        deductInventory(source.getMaterialCode(), source.getMaterialName(),
                source.getSupplierName(), stockArea, transferQty, currentOperator, now);

        // ==================== 8. 处理目标看板 ====================
        if (isMergeMode) {
            // ========== 合包：累加目标看板数量 ==========
            kanbanLabelRepository.increaseLabelQty(targetKanbanNo, transferQty);
            entityManager.flush();
            entityManager.clear(); // 确保后续查询读到最新数量
            // 重新查询目标看板以获取最新数量
            targetLabel = kanbanLabelRepository.findByKanbanNo(targetKanbanNo).orElse(targetLabel);
            addInventory(source.getMaterialCode(), source.getMaterialName(),
                    source.getSupplierName(), stockArea, transferQty, currentOperator, now);
        } else {
            // ========== 拆包：创建新看板+入库单 ==========
            if (!userSpecified) {
                targetKanbanNo = generateTargetKanbanNo(source.getKanbanNo());
            }
            inboundDocNo = generateInboundDocNo();

            // 创建入库单
            InboundOrder inboundOrder = createInboundOrder(source, transferQty, inboundDocNo, targetKanbanNo, currentOperator, now);
            inboundOrder = inboundOrderRepository.save(inboundOrder); // 保存后获得真实ID

            // 创建入库明细
            InboundOrderDetail inboundDetail = createInboundDetail(source, inboundDocNo, transferQty, stockArea, currentOperator, now);
            inboundDetail.setInboundOrderId(inboundOrder.getId()); // 设置真实FK
            inboundOrderDetailRepository.save(inboundDetail);

            // 创建目标看板标签
            InboundKanbanLabel target = createTargetKanban(source, targetKanbanNo, transferQty, inboundOrder.getId(), inboundDetail.getId(), inboundDocNo, currentOperator, now);
            kanbanLabelRepository.save(target);

            // 增加库存
            addInventory(source.getMaterialCode(), source.getMaterialName(),
                    source.getSupplierName(), stockArea, transferQty, currentOperator, now);
        }

        // ==================== 9. 记录转包历史（freshSource 已在步骤5中获取并更新） ====================
        int sourceQtyAfter = freshSource.getLabelQty() != null ? freshSource.getLabelQty() : 0;

        PackageTransfer record = createTransferRecord(
                source, targetKanbanNo, transferQty, source.getLabelQty(), sourceQtyAfter,
                isMergeMode ? "合包" : "拆包",
                outboundDocNo, inboundDocNo, currentOperator, now
        );
        transferRepository.save(record);

        // ==================== 11. 返回结果 ====================
        return new TransferResultDTO(
                source.getKanbanNo(),
                sourceQtyAfter,
                targetKanbanNo,
                isMergeMode ? targetLabel.getLabelQty() : transferQty,
                source.getMaterialCode(),
                source.getMaterialName(),
                source.getSupplierName(),
                stockArea,
                now,
                isMergeMode ? "合包" : "拆包",
                inboundDocNo
        );
    }

    // ==================== 合包目标校验 ====================

    private void validateMergeTarget(InboundKanbanLabel source, InboundKanbanLabel target) {
        // 物料一致性（必须）
        if (!source.getMaterialCode().equals(target.getMaterialCode())) {
            throw new IllegalArgumentException(
                    "目标看板物料号(" + target.getMaterialCode() +
                    ")与源看板(" + source.getMaterialCode() + ")不一致，不能合包");
        }
        // 供应商一致性（建议校验）
        if (!source.getSupplierCode().equals(target.getSupplierCode())) {
            throw new IllegalStateException(
                    "目标看板供应商与源看板不一致，建议确认后操作");
        }
        // 目标状态=已入库（必须）
        if (!"已入库".equals(target.getLabelStatus())) {
            throw new IllegalStateException("目标看板状态不是已入库，不能合包");
        }
        // 目标未封存（必须）
        if (Boolean.TRUE.equals(target.getSealed())) {
            throw new IllegalStateException("目标看板已封存，不能合包");
        }
        // 目标≠源（必须）
        if (target.getKanbanNo().equals(source.getKanbanNo())) {
            throw new IllegalArgumentException("不能对源看板自身进行转包");
        }
        // 目标不是已转包状态（建议）
        if ("已转包".equals(target.getTransferStatus())) {
            throw new IllegalStateException("目标看板已全部转包，不能作为合包目标");
        }
    }

    // ==================== 出库单创建 ====================

    private OutboundOrder createOutboundOrder(InboundKanbanLabel source, int transferQty,
            String outboundDocNo, boolean isMergeMode, String targetKanbanNo,
            String operator, LocalDateTime now) {
        OutboundOrder order = new OutboundOrder();
        order.setDocNo(outboundDocNo);
        order.setSupplier(source.getSupplierName());
        order.setStatus("已完成");
        order.setItemCount(1);
        order.setPlannedTotalQty(transferQty);
        order.setActualTotalQty(transferQty);
        order.setOutboundType(isMergeMode ? "合包出库" : "拆包出库");
        order.setRemark(buildOutboundRemark(source.getKanbanNo(), targetKanbanNo, isMergeMode));
        order.setCreatedBy(operator);
        order.setUpdatedBy(operator);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private OutboundOrderDetail createOutboundDetail(InboundKanbanLabel source, int transferQty,
            String outboundDocNo, boolean isMergeMode, String targetKanbanNo,
            String operator, LocalDateTime now) {
        OutboundOrderDetail detail = new OutboundOrderDetail();
        // FK将由调用方在保存出库单后设置
        detail.setDocNo(outboundDocNo);
        detail.setLineNo(1);
        detail.setSupplierCode(source.getSupplierCode());
        detail.setSupplierName(source.getSupplierName());
        detail.setMaterialCode(source.getMaterialCode());
        detail.setMaterialName(source.getMaterialName());
        detail.setPlannedQty(transferQty);
        detail.setActualQty(transferQty);
        detail.setWarehouseArea(getStockArea(source));
        detail.setRemark(buildOutboundDetailRemark(source.getKanbanNo(), targetKanbanNo, isMergeMode));
        detail.setCreatedBy(operator);
        detail.setUpdatedBy(operator);
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);
        return detail;
    }

    private String buildOutboundRemark(String sourceKanban, String targetKanban, boolean isMerge) {
        if (isMerge) {
            return String.format("合包：源看板 %s → 目标看板 %s", sourceKanban, targetKanban);
        } else {
            return String.format("拆包：源看板 %s → 新建目标看板 %s", sourceKanban, targetKanban);
        }
    }

    private String buildOutboundDetailRemark(String sourceKanban, String targetKanban, boolean isMerge) {
        if (isMerge) {
            return String.format("合包操作|源:%s|目标:%s", sourceKanban, targetKanban);
        } else {
            return String.format("拆包操作|源:%s|新建目标:%s", sourceKanban, targetKanban);
        }
    }

    // ==================== 入库单创建（拆包模式） ====================

    private InboundOrder createInboundOrder(InboundKanbanLabel source, int transferQty,
            String inboundDocNo, String targetKanbanNo,
            String operator, LocalDateTime now) {
        InboundOrder order = new InboundOrder();
        order.setDocNo(inboundDocNo);
        order.setSupplier(source.getSupplierName());
        order.setStatus("已完成");
        order.setItemCount(1);
        order.setPlannedTotalQty(transferQty);
        order.setActualTotalQty(transferQty);
        order.setRemark("拆包目标看板: " + targetKanbanNo);
        order.setCreatedBy(operator);
        order.setUpdatedBy(operator);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private InboundOrderDetail createInboundDetail(InboundKanbanLabel source, String inboundDocNo,
            int transferQty, String stockArea,
            String operator, LocalDateTime now) {
        InboundOrderDetail detail = new InboundOrderDetail();
        // FK将由调用方在保存入库单后设置
        detail.setDocNo(inboundDocNo);
        detail.setLineNo(1);
        detail.setSupplierCode(source.getSupplierCode());
        detail.setSupplierName(source.getSupplierName());
        detail.setMaterialCode(source.getMaterialCode());
        detail.setMaterialName(source.getMaterialName());
        detail.setPackageModel(source.getPackageModel());
        detail.setPlannedQty(transferQty);
        detail.setActualQty(transferQty);
        detail.setPackageCount(1);
        detail.setWarehouseArea(stockArea);
        detail.setCreatedBy(operator);
        detail.setUpdatedBy(operator);
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);
        return detail;
    }

    // ==================== 目标看板创建（拆包模式） ====================

    private InboundKanbanLabel createTargetKanban(InboundKanbanLabel source, String kanbanNo,
            int qty, Long inboundOrderId, Long inboundDetailId, String inboundDocNo,
            String operator, LocalDateTime now) {
        InboundKanbanLabel target = new InboundKanbanLabel();
        target.setInboundOrderId(inboundOrderId);
        target.setInboundOrderDetailId(inboundDetailId);
        target.setDocNo(inboundDocNo);
        target.setKanbanNo(kanbanNo);
        target.setQrPayload("WMS-INBOUND|" + kanbanNo);
        target.setMaterialCode(source.getMaterialCode());
        target.setMaterialName(source.getMaterialName());
        target.setSupplierCode(source.getSupplierCode());
        target.setSupplierName(source.getSupplierName());
        target.setPackageModel(source.getPackageModel());
        target.setWarehouseArea(getStockArea(source));
        target.setLabelQty(qty);
        target.setPackageSeq(1);
        target.setPackageTotal(1);
        target.setLabelStatus("已入库");
        target.setTransferStatus("转入");
        target.setSealed(false);
        target.setCreatedBy(operator);
        target.setUpdatedBy(operator);
        target.setCreatedAt(now);
        target.setUpdatedAt(now);
        return target;
    }

    // ==================== 库存操作 ====================

    private void deductInventory(String materialCode, String materialName, String supplier, String stockArea,
            int qty, String operator, LocalDateTime now) {
        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplierAndWarehouseArea(materialCode, supplier, stockArea)
                .orElse(null);
        if (stock != null) {
            int newOnHand = Math.max(0, stock.getOnHandQty() - qty);
            stock.setOnHandQty(newOnHand);
            stock.setUpdatedBy(operator);
            stock.setUpdatedAt(now);
            inventoryStockRepository.save(stock);
        }
    }

    private void addInventory(String materialCode, String materialName, String supplier, String stockArea,
            int qty, String operator, LocalDateTime now) {
        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplierAndWarehouseArea(materialCode, supplier, stockArea)
                .orElse(null);
        if (stock != null) {
            stock.setOnHandQty(stock.getOnHandQty() + qty);
            stock.setUpdatedBy(operator);
            stock.setUpdatedAt(now);
            inventoryStockRepository.save(stock);
        } else {
            InventoryStock newStock = new InventoryStock();
            newStock.setMaterialCode(materialCode);
            newStock.setMaterialName(materialName);
            newStock.setSupplier(supplier);
            newStock.setWarehouseArea(stockArea);
            newStock.setOnHandQty(qty);
            newStock.setCreatedBy(operator);
            newStock.setUpdatedBy(operator);
            newStock.setCreatedAt(now);
            newStock.setUpdatedAt(now);
            inventoryStockRepository.save(newStock);
        }
    }

    // ==================== 转包历史 ====================

    private PackageTransfer createTransferRecord(InboundKanbanLabel source, String targetKanbanNo,
            int transferQty, int qtyBefore, int qtyAfter, String transferType,
            String outboundDocNo, String inboundDocNo,
            String operator, LocalDateTime now) {
        PackageTransfer record = new PackageTransfer();
        record.setSourceKanbanNo(source.getKanbanNo());
        record.setTargetKanbanNo(targetKanbanNo);
        record.setTransferQty(transferQty);
        record.setSourceQtyBefore(qtyBefore);
        record.setSourceQtyAfter(qtyAfter);
        record.setMaterialCode(source.getMaterialCode());
        record.setMaterialName(source.getMaterialName());
        record.setSupplierName(source.getSupplierName());
        record.setOperator(operator);
        record.setCreatedAt(now);
        record.setSourceOutboundDocNo(outboundDocNo);
        record.setTargetInboundDocNo(inboundDocNo);
        record.setTransferType(transferType);
        return record;
    }

    // ==================== 工具方法 ====================

    private String getStockArea(InboundKanbanLabel label) {
        return label.getWarehouseArea() != null ? label.getWarehouseArea() : "默认库区";
    }

    private String generateTargetKanbanNo(String sourceKanbanNo) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortSrc = sourceKanbanNo.length() > 20
                ? sourceKanbanNo.substring(sourceKanbanNo.length() - 8)
                : sourceKanbanNo.replaceAll("[^a-zA-Z0-9]", "-");
        long seq = System.currentTimeMillis() % 100000;
        return "T-" + date + "-" + shortSrc + "-" + seq;
    }

    private synchronized String generateOutboundDocNo() {
        String timestamp = LocalDateTime.now().format(DOC_NO_FORMATTER);
        for (int attempt = 0; attempt < 1000; attempt++) {
            int suffix = DOC_NO_SEQUENCE.updateAndGet(current -> (current + 1) % 1000);
            String docNo = "OUT" + timestamp + String.format("%03d", suffix);
            if (!outboundOrderRepository.existsByDocNo(docNo)) {
                return docNo;
            }
        }
        throw new IllegalStateException("出库单号生成失败，请稍后重试");
    }

    private synchronized String generateInboundDocNo() {
        String timestamp = LocalDateTime.now().format(DOC_NO_FORMATTER);
        for (int attempt = 0; attempt < 1000; attempt++) {
            int suffix = DOC_NO_SEQUENCE.updateAndGet(current -> (current + 1) % 1000);
            String docNo = "IN" + timestamp + String.format("%03d", suffix);
            if (!inboundOrderRepository.existsByDocNo(docNo)) {
                return docNo;
            }
        }
        throw new IllegalStateException("入库单号生成失败，请稍后重试");
    }

    private String normalizeOperator(String operator) {
        return operator != null ? operator.trim() : "system";
    }

    // ==================== 列表查询 ====================

    @Override
    public TransferKanbanPageResponse listAvailableKanbans(String materialCode, String supplierName, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<InboundKanbanLabel> pageResult = buildKanbanQuery(materialCode, supplierName, pageable);

        List<TransferKanbanDTO> records = pageResult.getContent().stream()
                .map(this::toKanbanDTO)
                .collect(Collectors.toList());

        return new TransferKanbanPageResponse(
                pageResult.getTotalElements(), safePage, safeSize, records);
    }

    private Page<InboundKanbanLabel> buildKanbanQuery(String materialCode, String supplierName, Pageable pageable) {
        boolean hasMaterial = materialCode != null && !materialCode.trim().isEmpty();
        boolean hasSupplier = supplierName != null && !supplierName.trim().isEmpty();

        if (hasMaterial && hasSupplier) {
            return kanbanLabelRepository
                    .findByLabelStatusAndSealedFalseAndMaterialCodeContainingAndSupplierNameContaining(
                            "已入库", materialCode.trim(), supplierName.trim(), pageable);
        } else if (hasMaterial) {
            return kanbanLabelRepository
                    .findByLabelStatusAndSealedFalseAndMaterialCodeContaining(
                            "已入库", materialCode.trim(), pageable);
        } else if (hasSupplier) {
            return kanbanLabelRepository
                    .findByLabelStatusAndSealedFalseAndSupplierNameContaining(
                            "已入库", supplierName.trim(), pageable);
        } else {
            return kanbanLabelRepository.findByLabelStatusAndSealedFalse("已入库", pageable);
        }
    }

    private TransferKanbanDTO toKanbanDTO(InboundKanbanLabel label) {
        TransferKanbanDTO dto = new TransferKanbanDTO();
        dto.setId(label.getId());
        dto.setKanbanNo(label.getKanbanNo());
        dto.setDocNo(label.getDocNo());
        dto.setMaterialCode(label.getMaterialCode());
        dto.setMaterialName(label.getMaterialName());
        dto.setSupplierCode(label.getSupplierCode());
        dto.setSupplierName(label.getSupplierName());
        dto.setPackageModel(label.getPackageModel());
        dto.setWarehouseArea(label.getWarehouseArea());
        dto.setLabelQty(label.getLabelQty());
        dto.setLabelStatus(label.getLabelStatus());
        dto.setSealed(label.getSealed());
        dto.setTransferStatus(label.getTransferStatus());
        dto.setCreatedAt(label.getCreatedAt());

        dto.setAvailableQty(calculateAvailableQty(label));

        return dto;
    }

    private int calculateAvailableQty(InboundKanbanLabel label) {
        int labelQty = label.getLabelQty() == null ? 0 : label.getLabelQty();
        int frozenQty = label.getFrozenQty() == null ? 0 : label.getFrozenQty();
        int consumedQty = outboundHistoryRepository.findByKanbanLabelId(label.getId())
                .stream()
                .filter(h -> !"已退库".equals(h.getStatus()))
                .mapToInt(h -> h.getIssueQty() == null ? 0 : h.getIssueQty())
                .sum();
        return Math.max(0, labelQty - consumedQty - frozenQty);
    }

    @Override
    public TransferHistoryPageResponse listTransferHistory(String sourceKanbanNo, String targetKanbanNo, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PackageTransfer> pageResult = buildHistoryQuery(sourceKanbanNo, targetKanbanNo, pageable);

        List<TransferHistoryDTO> records = pageResult.getContent().stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());

        return new TransferHistoryPageResponse(
                pageResult.getTotalElements(), safePage, safeSize, records);
    }

    private Page<PackageTransfer> buildHistoryQuery(String source, String target, Pageable pageable) {
        boolean hasSource = source != null && !source.trim().isEmpty();
        boolean hasTarget = target != null && !target.trim().isEmpty();

        if (hasSource && hasTarget) {
            return transferRepository
                    .findBySourceKanbanNoContainingAndTargetKanbanNoContainingOrderByCreatedAtDesc(
                            source.trim(), target.trim(), pageable);
        } else if (hasSource) {
            return transferRepository.findBySourceKanbanNoOrderByCreatedAtDesc(source.trim(), pageable);
        } else if (hasTarget) {
            return transferRepository.findByTargetKanbanNoOrderByCreatedAtDesc(target.trim(), pageable);
        } else {
            return transferRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    private TransferHistoryDTO toHistoryDTO(PackageTransfer record) {
        return new TransferHistoryDTO(
                record.getId(),
                record.getSourceKanbanNo(),
                record.getTargetKanbanNo(),
                record.getTransferQty(),
                record.getSourceQtyBefore(),
                record.getSourceQtyAfter(),
                record.getMaterialCode(),
                record.getMaterialName(),
                record.getSupplierName(),
                record.getOperator(),
                record.getCreatedAt(),
                record.getSourceOutboundDocNo(),
                record.getTargetInboundDocNo(),
                record.getTransferType()
        );
    }
}
