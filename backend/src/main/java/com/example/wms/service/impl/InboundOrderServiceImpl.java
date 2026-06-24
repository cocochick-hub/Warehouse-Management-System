package com.example.wms.service.impl;

import com.example.wms.dto.inbound.*;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.entity.InventoryStock;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.InboundOrderDetailRepository;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.service.InboundOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class InboundOrderServiceImpl implements InboundOrderService {

    private static final String STATUS_PENDING = "未入库";
    private static final String STATUS_PARTIAL = "部分完成";
    private static final String STATUS_COMPLETED = "已完成";
    private static final String LABEL_STATUS_PENDING = "未入库";
    private static final String LABEL_STATUS_RECEIVED = "已入库";
    private static final String QR_PREFIX = "WMS-INBOUND|";
    private static final String DEFAULT_WAREHOUSE_AREA = "默认库区";
    private static final String DEFAULT_TRANSFER_STATUS = "不转包";
    private static final DateTimeFormatter DOC_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter KANBAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final AtomicInteger DOC_NO_SEQUENCE = new AtomicInteger(0);

    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderDetailRepository inboundOrderDetailRepository;
    private final InboundKanbanLabelRepository inboundKanbanLabelRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public InboundOrderServiceImpl(InboundOrderRepository inboundOrderRepository,
                                   InboundOrderDetailRepository inboundOrderDetailRepository,
                                   InboundKanbanLabelRepository inboundKanbanLabelRepository,
                                   InventoryStockRepository inventoryStockRepository) {
        this.inboundOrderRepository = inboundOrderRepository;
        this.inboundOrderDetailRepository = inboundOrderDetailRepository;
        this.inboundKanbanLabelRepository = inboundKanbanLabelRepository;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    @Override
    public InboundOrderPageResponse listOrders(String docNo, String supplier, String status, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<InboundOrder> specification = buildOrderSpecification(docNo, supplier, status);
        Page<InboundOrder> resultPage = inboundOrderRepository.findAll(specification, pageable);
        List<InboundOrderSummaryDTO> records = resultPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());

        return new InboundOrderPageResponse((int) resultPage.getTotalElements(), safePage, safeSize, records);
    }

    @Override
    public InboundOrderDetailResponse getOrderDetail(Long id) {
        InboundOrder order = findOrder(id);
        List<InboundOrderDetail> details = inboundOrderDetailRepository.findByInboundOrderIdOrderByLineNoAsc(id);
        return new InboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details), toInventoryStockDTOs(order, details));
    }

    @Override
    @Transactional
    public InboundOrderDetailResponse createOrder(InboundOrderCreateRequest request, String operator) {
        validateCreateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        String docNo = generateDocNo();

        InboundOrder order = new InboundOrder();
        order.setDocNo(docNo);
        order.setSupplier(resolveOrderSupplier(request));
        order.setStatus(STATUS_PENDING);
        order.setItemCount(request.getDetails().size());
        order.setPlannedTotalQty(sumPlannedQty(request.getDetails()));
        order.setActualTotalQty(0);
        order.setTransferStatus(resolveOrderTransferStatus(request));
        order.setRemark(trimToNull(request.getRemark()));
        order.setCreatedBy(currentOperator);
        order.setUpdatedBy(currentOperator);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        InboundOrder savedOrder = inboundOrderRepository.saveAndFlush(order);

        List<InboundOrderDetail> details = new ArrayList<>();
        int lineNo = 1;
        for (InboundOrderCreateDetailRequest item : request.getDetails()) {
            InboundOrderDetail detail = new InboundOrderDetail();
            detail.setInboundOrderId(savedOrder.getId());
            detail.setDocNo(savedOrder.getDocNo());
            detail.setLineNo(lineNo++);
            detail.setSupplierCode(item.getSupplierCode().trim());
            detail.setSupplierName(item.getSupplierName().trim());
            detail.setMaterialCode(item.getMaterialCode().trim());
            detail.setMaterialName(item.getMaterialName().trim());
            detail.setPackageModel(trimToNull(item.getPackageModel()));
            detail.setPackagingCapacity(safeInt(item.getPackagingCapacity()));
            detail.setPlannedQty(item.getPlannedQty());
            detail.setActualQty(0);
            detail.setPackageCount(calculatePackageCount(item.getPlannedQty(), item.getPackagingCapacity()));
            detail.setWarehouseArea(defaultIfBlank(item.getWarehouseArea(), DEFAULT_WAREHOUSE_AREA));
            detail.setTransferStatus(defaultIfBlank(item.getTransferStatus(), DEFAULT_TRANSFER_STATUS));
            detail.setRemark(trimToNull(item.getRemark()));
            detail.setCreatedBy(currentOperator);
            detail.setUpdatedBy(currentOperator);
            detail.setCreatedAt(now);
            detail.setUpdatedAt(now);
            details.add(detail);
        }

        inboundOrderDetailRepository.saveAll(details);
        return getOrderDetail(savedOrder.getId());
    }

    @Override
    @Transactional
    public InboundOrderDetailResponse receiveOrder(Long id, InboundReceiveRequest request, String operator) {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("入库明细不能为空");
        }

        InboundOrder order = findOrder(id);
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("已完成单据不允许再次入库");
        }

        List<InboundOrderDetail> details = inboundOrderDetailRepository.findByInboundOrderIdOrderByLineNoAsc(id);
        if (details.isEmpty()) {
            throw new IllegalStateException("当前入库单缺少明细，无法执行入库");
        }

        Map<Long, InboundReceiveDetailRequest> requestMap = new HashMap<>();
        for (InboundReceiveDetailRequest item : request.getDetails()) {
            if (requestMap.put(item.getDetailId(), item) != null) {
                throw new IllegalArgumentException("同一明细不能重复提交入库数量");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        Set<Long> detailIds = details.stream().map(InboundOrderDetail::getId).collect(Collectors.toSet());
        for (Long detailId : requestMap.keySet()) {
            if (!detailIds.contains(detailId)) {
                throw new IllegalArgumentException("存在不属于当前入库单的明细");
            }
        }

        int totalReceiveQty = requestMap.values().stream()
                .map(InboundReceiveDetailRequest::getReceiveQty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (totalReceiveQty <= 0) {
            throw new IllegalArgumentException("至少需要一条明细的入库数量大于0");
        }

        Map<Long, Integer> receiveQtyByDetailId = new HashMap<>();
        for (InboundOrderDetail detail : details) {
            InboundReceiveDetailRequest item = requestMap.get(detail.getId());
            if (item == null) {
                continue;
            }

            int receiveQty = item.getReceiveQty() == null ? 0 : item.getReceiveQty();
            int nextActualQty = safeInt(detail.getActualQty()) + receiveQty;
            if (nextActualQty > safeInt(detail.getPlannedQty())) {
                throw new IllegalArgumentException("物料 " + detail.getMaterialCode() + " 的累计实收数量不能大于计划数量");
            }

            receiveQtyByDetailId.put(detail.getId(), receiveQty);
            detail.setActualQty(nextActualQty);
            detail.setUpdatedBy(currentOperator);
            detail.setUpdatedAt(now);
        }

        inboundOrderDetailRepository.saveAll(details);
        updateInventoryStocks(order, details, receiveQtyByDetailId, currentOperator, now);
        // 手动入库时自动生成看板标签（标记为已入库）
        createKanbanLabelsForReceive(order, details, receiveQtyByDetailId, currentOperator, now);

        order.setItemCount(details.size());
        order.setPlannedTotalQty(details.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum());
        order.setActualTotalQty(details.stream().mapToInt(item -> safeInt(item.getActualQty())).sum());
        order.setStatus(calculateStatus(order.getPlannedTotalQty(), order.getActualTotalQty()));
        order.setUpdatedBy(currentOperator);
        order.setUpdatedAt(now);
        inboundOrderRepository.save(order);

        return new InboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details), toInventoryStockDTOs(order, details));
    }

    @Override
    @Transactional
    public List<InboundKanbanLabelDTO> generateKanbanLabels(Long id, String operator) {
        InboundOrder order = findOrder(id);
        List<InboundOrderDetail> details = inboundOrderDetailRepository.findByInboundOrderIdOrderByLineNoAsc(id);
        if (details.isEmpty()) {
            throw new IllegalStateException("当前入库单缺少明细，无法生成看板");
        }

        List<InboundKanbanLabel> existingLabels = inboundKanbanLabelRepository.findByInboundOrderIdOrderByInboundOrderDetailIdAscPackageSeqAsc(id);
        if (!existingLabels.isEmpty()) {
            return toKanbanLabelDTOs(existingLabels);
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        List<InboundKanbanLabel> labels = new ArrayList<>();
        for (InboundOrderDetail detail : details) {
            int plannedQty = safeInt(detail.getPlannedQty());
            int pendingQty = Math.max(plannedQty - safeInt(detail.getActualQty()), 0);
            if (pendingQty <= 0) {
                continue;
            }
            int capacity = safeInt(detail.getPackagingCapacity());
            int packageTotal = calculatePackageCount(pendingQty, capacity);
            detail.setPackageCount(packageTotal);
            detail.setUpdatedBy(currentOperator);
            detail.setUpdatedAt(now);

            int remainingQty = pendingQty;
            for (int packageSeq = 1; packageSeq <= packageTotal; packageSeq++) {
                int labelQty = calculateLabelQty(remainingQty, capacity);
                remainingQty -= labelQty;

                InboundKanbanLabel label = new InboundKanbanLabel();
                label.setInboundOrderId(order.getId());
                label.setInboundOrderDetailId(detail.getId());
                label.setDocNo(order.getDocNo());
                label.setKanbanNo(generateKanbanNo(order, detail, packageSeq));
                label.setQrPayload(QR_PREFIX + label.getKanbanNo());
                label.setMaterialCode(detail.getMaterialCode());
                label.setMaterialName(detail.getMaterialName());
                label.setSupplierCode(detail.getSupplierCode());
                label.setSupplierName(detail.getSupplierName());
                label.setPackageModel(detail.getPackageModel());
                label.setWarehouseArea(defaultIfBlank(detail.getWarehouseArea(), DEFAULT_WAREHOUSE_AREA));
                label.setLabelQty(labelQty);
                label.setPackageSeq(packageSeq);
                label.setPackageTotal(packageTotal);
                label.setTransferStatus(defaultIfBlank(detail.getTransferStatus(), DEFAULT_TRANSFER_STATUS));
                label.setLabelStatus(LABEL_STATUS_PENDING);
                label.setCreatedBy(currentOperator);
                label.setUpdatedBy(currentOperator);
                label.setCreatedAt(now);
                label.setUpdatedAt(now);
                labels.add(label);
            }
        }

        inboundOrderDetailRepository.saveAll(details);
        if (labels.isEmpty()) {
            throw new IllegalStateException("当前入库单没有待入库数量，无法生成看板");
        }
        return toKanbanLabelDTOs(inboundKanbanLabelRepository.saveAll(labels));
    }

    @Override
    public List<InboundKanbanLabelDTO> listKanbanLabels(Long id) {
        if (!inboundOrderRepository.existsById(id)) {
            throw new EntityNotFoundException("入库单不存在");
        }
        return toKanbanLabelDTOs(inboundKanbanLabelRepository.findByInboundOrderIdOrderByInboundOrderDetailIdAscPackageSeqAsc(id));
    }

    @Override
    public InboundKanbanLabelDTO getScanLabel(String kanbanNoOrPayload) {
        InboundKanbanLabel label = findLabelByScanValue(kanbanNoOrPayload);
        return toKanbanLabelDTO(label);
    }

    @Override
    @Transactional
    public InboundOrderDetailResponse receiveByScan(InboundScanReceiveRequest request, String operator) {
        InboundKanbanLabel label = findLabelByScanValue(request.getKanbanNo());
        if (!LABEL_STATUS_PENDING.equals(label.getLabelStatus())) {
            throw new IllegalStateException("该看板已入库或不可入库");
        }

        InboundOrder order = findOrder(label.getInboundOrderId());
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("已完成单据不允许再次入库");
        }

        InboundOrderDetail detail = inboundOrderDetailRepository.findById(label.getInboundOrderDetailId())
                .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));

        int labelQty = safeInt(label.getLabelQty());
        int nextActualQty = safeInt(detail.getActualQty()) + labelQty;
        if (labelQty <= 0) {
            throw new IllegalStateException("看板数量异常，无法入库");
        }
        if (nextActualQty > safeInt(detail.getPlannedQty())) {
            throw new IllegalArgumentException("物料 " + detail.getMaterialCode() + " 的累计实收数量不能大于计划数量");
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        detail.setActualQty(nextActualQty);
        detail.setUpdatedBy(currentOperator);
        detail.setUpdatedAt(now);
        inboundOrderDetailRepository.save(detail);

        label.setLabelStatus(LABEL_STATUS_RECEIVED);
        label.setReceivedAt(now);
        label.setReceivedBy(currentOperator);
        label.setUpdatedBy(currentOperator);
        label.setUpdatedAt(now);
        inboundKanbanLabelRepository.save(label);

        Map<Long, Integer> receiveQtyByDetailId = new HashMap<>();
        receiveQtyByDetailId.put(detail.getId(), labelQty);
        updateInventoryStocks(order, Collections.singletonList(detail), receiveQtyByDetailId, currentOperator, now);

        List<InboundOrderDetail> allDetails = inboundOrderDetailRepository.findByInboundOrderIdOrderByLineNoAsc(order.getId());
        updateOrderSummary(order, allDetails, currentOperator, now);
        inboundOrderRepository.save(order);

        return new InboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(allDetails), toInventoryStockDTOs(order, allDetails));
    }

    @Override
    public InboundOrderPageResponse listHistory(String docNo, String supplier, String materialCode,
                                                 String transferStatus, String warehouseArea,
                                                 Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        String docNoKeyword = trimToNull(docNo);
        String supplierKeyword = trimToNull(supplier);
        String mtlKeyword = trimToNull(materialCode);
        String tsKeyword = trimToNull(transferStatus);
        String waKeyword = trimToNull(warehouseArea);

        Specification<InboundOrder> orderSpec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Only show orders that have been actually received
            predicates.add(criteriaBuilder.greaterThan(root.get("actualTotalQty"), 0));
            if (docNoKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("docNo"), "%" + docNoKeyword + "%"));
            }
            if (supplierKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("supplier"), "%" + supplierKeyword + "%"));
            }

            // When detail-level filters are present, build a subquery to find matching order IDs
            if (mtlKeyword != null || tsKeyword != null || waKeyword != null) {
                Subquery<Long> detailSubquery = query.subquery(Long.class);
                Root<InboundOrderDetail> detailRoot = detailSubquery.from(InboundOrderDetail.class);
                List<Predicate> detailPreds = new ArrayList<>();
                detailPreds.add(criteriaBuilder.equal(detailRoot.get("inboundOrderId"), root.get("id")));
                if (mtlKeyword != null) {
                    detailPreds.add(criteriaBuilder.like(detailRoot.get("materialCode"), "%" + mtlKeyword + "%"));
                }
                if (tsKeyword != null) {
                    detailPreds.add(criteriaBuilder.equal(detailRoot.get("transferStatus"), tsKeyword));
                }
                if (waKeyword != null) {
                    detailPreds.add(criteriaBuilder.like(detailRoot.get("warehouseArea"), "%" + waKeyword + "%"));
                }
                detailSubquery.select(detailRoot.get("id"))
                        .where(detailPreds.toArray(new Predicate[0]));
                predicates.add(criteriaBuilder.exists(detailSubquery));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<InboundOrder> resultPage = inboundOrderRepository.findAll(orderSpec, pageable);
        List<InboundOrderSummaryDTO> records = resultPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());

        return new InboundOrderPageResponse((int) resultPage.getTotalElements(), safePage, safeSize, records);
    }

    private InboundOrder findOrder(Long id) {
        return inboundOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("入库单不存在"));
    }

    private void validateCreateRequest(InboundOrderCreateRequest request) {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("入库单至少包含一条明细");
        }

        Set<String> supplierCodes = new HashSet<>();
        Set<String> supplierMaterialKeys = new HashSet<>();
        for (InboundOrderCreateDetailRequest detail : request.getDetails()) {
            String supplierCode = trimToNull(detail.getSupplierCode());
            if (supplierCode == null) {
                throw new IllegalArgumentException("供应商代码不能为空");
            }
            String supplierName = trimToNull(detail.getSupplierName());
            if (supplierName == null) {
                throw new IllegalArgumentException("供应商名称不能为空");
            }
            String materialCode = trimToNull(detail.getMaterialCode());
            if (materialCode == null) {
                throw new IllegalArgumentException("物料号不能为空");
            }
            String key = supplierCode + "::" + materialCode;
            if (!supplierMaterialKeys.add(key)) {
                throw new IllegalArgumentException("同一张入库单中不允许重复选择同一供应商下的同一物料");
            }
            supplierCodes.add(supplierCode);
        }

        if (supplierCodes.size() > 1) {
            throw new IllegalArgumentException("一张入库单只能包含同一个供应商的物料，请分批创建");
        }
    }

    private synchronized String generateDocNo() {
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

    private int sumPlannedQty(List<InboundOrderCreateDetailRequest> details) {
        return details.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum();
    }

    private String resolveOrderSupplier(InboundOrderCreateRequest request) {
        String requestSupplier = trimToNull(request.getSupplier());
        if (requestSupplier != null) {
            return requestSupplier;
        }

        return request.getDetails().stream()
                .map(InboundOrderCreateDetailRequest::getSupplierName)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("未知供应商");
    }

    private String resolveOrderTransferStatus(InboundOrderCreateRequest request) {
        String requestTransferStatus = trimToNull(request.getTransferStatus());
        if (requestTransferStatus != null) {
            return requestTransferStatus;
        }

        return request.getDetails().stream()
                .map(InboundOrderCreateDetailRequest::getTransferStatus)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(DEFAULT_TRANSFER_STATUS);
    }

    private int calculatePackageCount(Integer plannedQty, Integer packagingCapacity) {
        int planned = safeInt(plannedQty);
        int capacity = safeInt(packagingCapacity);
        if (planned <= 0 || capacity <= 0) {
            return 1;
        }
        return (planned + capacity - 1) / capacity;
    }

    private double calculateBoxCount(Integer qty, Integer packagingCapacity) {
        int quantity = safeInt(qty);
        int capacity = safeInt(packagingCapacity);
        if (quantity <= 0 || capacity <= 0) {
            return 0D;
        }
        return Math.round(quantity * 10D / capacity) / 10D;
    }

    private int calculateLabelQty(int remainingQty, int packagingCapacity) {
        if (remainingQty <= 0) {
            return 0;
        }
        if (packagingCapacity <= 0) {
            return remainingQty;
        }
        return Math.min(remainingQty, packagingCapacity);
    }

    /** 计算指定物料+供应商已封存的看板总数 */
    private int calculateSealedQtyByMaterial(String materialCode, String supplierName) {
        return inboundKanbanLabelRepository
                .findByMaterialCodeAndSupplierNameAndSealedTrue(materialCode, supplierName)
                .stream()
                .mapToInt(label -> safeInt(label.getLabelQty()))
                .sum();
    }

    private String generateKanbanNo(InboundOrder order, InboundOrderDetail detail, int packageSeq) {
        String date = LocalDateTime.now().format(KANBAN_DATE_FORMATTER);
        String safeMaterialCode = detail.getMaterialCode().replaceAll("[^A-Za-z0-9-]", "");
        for (int attempt = 0; attempt < 100; attempt++) {
            String kanbanNo = "R-" + date + "-" + order.getDocNo() + "-" + safeMaterialCode + "-" + detail.getLineNo() + "-" + packageSeq;
            if (attempt > 0) {
                kanbanNo = kanbanNo + "-" + attempt;
            }
            if (!inboundKanbanLabelRepository.existsByKanbanNo(kanbanNo)) {
                return kanbanNo;
            }
        }
        throw new IllegalStateException("看板号生成失败，请稍后重试");
    }

    /** 手动入库时自动生成已入库的看板标签 */
    private void createKanbanLabelsForReceive(InboundOrder order,
                                               List<InboundOrderDetail> details,
                                               Map<Long, Integer> receiveQtyByDetailId,
                                               String operator,
                                               LocalDateTime now) {
        List<InboundKanbanLabel> labels = new ArrayList<>();
        for (InboundOrderDetail detail : details) {
            int receiveQty = receiveQtyByDetailId.getOrDefault(detail.getId(), 0);
            if (receiveQty <= 0) continue;

            // 已有看板则将其状态更新为已入库
            List<InboundKanbanLabel> existing = inboundKanbanLabelRepository
                    .findByInboundOrderDetailIdIn(Collections.singletonList(detail.getId()));
            if (!existing.isEmpty()) {
                for (InboundKanbanLabel existingLabel : existing) {
                    existingLabel.setLabelStatus("已入库");
                    existingLabel.setReceivedAt(now);
                    existingLabel.setReceivedBy(operator);
                    existingLabel.setUpdatedBy(operator);
                    existingLabel.setUpdatedAt(now);
                }
                inboundKanbanLabelRepository.saveAll(existing);
                continue;
            }

            int capacity = safeInt(detail.getPackagingCapacity());
            if (capacity <= 0 || capacity > receiveQty) {
                capacity = receiveQty;
            }
            int packageTotal = (receiveQty + capacity - 1) / capacity;

            int remainingQty = receiveQty;
            for (int packageSeq = 1; packageSeq <= packageTotal; packageSeq++) {
                int labelQty = calculateLabelQty(remainingQty, capacity);
                remainingQty -= labelQty;

                InboundKanbanLabel label = new InboundKanbanLabel();
                label.setInboundOrderId(order.getId());
                label.setInboundOrderDetailId(detail.getId());
                label.setDocNo(order.getDocNo());
                label.setKanbanNo(generateKanbanNo(order, detail, packageSeq));
                label.setQrPayload("QR:" + label.getKanbanNo());
                label.setMaterialCode(detail.getMaterialCode());
                label.setMaterialName(detail.getMaterialName());
                label.setSupplierCode(detail.getSupplierCode());
                label.setSupplierName(detail.getSupplierName());
                label.setPackageModel(detail.getPackageModel());
                label.setWarehouseArea(defaultIfBlank(detail.getWarehouseArea(), "默认库区"));
                label.setLabelQty(labelQty);
                label.setPackageSeq(packageSeq);
                label.setPackageTotal(packageTotal);
                label.setTransferStatus(defaultIfBlank(detail.getTransferStatus(), "不转包"));
                label.setLabelStatus("已入库");
                label.setReceivedAt(now);
                label.setReceivedBy(operator);
                label.setCreatedBy(operator);
                label.setUpdatedBy(operator);
                label.setCreatedAt(now);
                label.setUpdatedAt(now);
                labels.add(label);
            }
        }
        if (!labels.isEmpty()) {
            inboundKanbanLabelRepository.saveAll(labels);
        }
    }

    private void updateInventoryStocks(InboundOrder order,
                                       List<InboundOrderDetail> details,
                                       Map<Long, Integer> receiveQtyByDetailId,
                                       String operator,
                                       LocalDateTime now) {
        List<InventoryStock> stocksToSave = new ArrayList<>();
        for (InboundOrderDetail detail : details) {
            int receiveQty = receiveQtyByDetailId.getOrDefault(detail.getId(), 0);
            if (receiveQty <= 0) {
                continue;
            }

            // 按物料号+供应商+库区定位库存行，确保不同库区各自累加
            String warehouseArea = defaultIfBlank(detail.getWarehouseArea(), DEFAULT_WAREHOUSE_AREA);
            InventoryStock stock = inventoryStockRepository
                    .findByMaterialCodeAndSupplierAndWarehouseArea(detail.getMaterialCode(), detail.getSupplierName(), warehouseArea)
                    .orElseGet(() -> createInventoryStock(order, detail, operator, now));

            stock.setMaterialName(detail.getMaterialName());
            stock.setOnHandQty(safeInt(stock.getOnHandQty()) + receiveQty);
            stock.setLastInboundDocNo(order.getDocNo());
            stock.setLastInboundAt(now);
            stock.setTransferStatus(defaultIfBlank(detail.getTransferStatus(), DEFAULT_TRANSFER_STATUS));
            stock.setWarehouseArea(warehouseArea);
            stock.setUpdatedBy(operator);
            stock.setUpdatedAt(now);
            stocksToSave.add(stock);
        }

        if (!stocksToSave.isEmpty()) {
            inventoryStockRepository.saveAll(stocksToSave);
        }
    }

    private InventoryStock createInventoryStock(InboundOrder order,
                                                InboundOrderDetail detail,
                                                String operator,
                                                LocalDateTime now) {
        InventoryStock stock = new InventoryStock();
        stock.setMaterialCode(detail.getMaterialCode());
        stock.setSupplier(detail.getSupplierName());
        stock.setOnHandQty(0);
        stock.setLastInboundDocNo(null);
        stock.setLastInboundAt(null);
        stock.setTransferStatus(defaultIfBlank(detail.getTransferStatus(), DEFAULT_TRANSFER_STATUS));
        stock.setWarehouseArea(defaultIfBlank(detail.getWarehouseArea(), DEFAULT_WAREHOUSE_AREA));
        stock.setCreatedBy(operator);
        stock.setUpdatedBy(operator);
        stock.setCreatedAt(now);
        stock.setUpdatedAt(now);
        return stock;
    }

    private void updateOrderSummary(InboundOrder order, List<InboundOrderDetail> details, String operator, LocalDateTime now) {
        order.setItemCount(details.size());
        order.setPlannedTotalQty(details.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum());
        order.setActualTotalQty(details.stream().mapToInt(item -> safeInt(item.getActualQty())).sum());
        order.setStatus(calculateStatus(order.getPlannedTotalQty(), order.getActualTotalQty()));
        order.setUpdatedBy(operator);
        order.setUpdatedAt(now);
    }

    private Specification<InboundOrder> buildOrderSpecification(String docNo, String supplier, String status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String docNoKeyword = trimToNull(docNo);
            String supplierKeyword = trimToNull(supplier);
            String statusKeyword = trimToNull(status);

            if (docNoKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("docNo"), "%" + docNoKeyword + "%"));
            }
            if (supplierKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("supplier"), "%" + supplierKeyword + "%"));
            }
            if (statusKeyword != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), statusKeyword));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String calculateStatus(Integer plannedTotalQty, Integer actualTotalQty) {
        int planned = safeInt(plannedTotalQty);
        int actual = safeInt(actualTotalQty);
        if (actual <= 0) {
            return STATUS_PENDING;
        }
        if (actual >= planned) {
            return STATUS_COMPLETED;
        }
        return STATUS_PARTIAL;
    }

    private InboundOrderSummaryDTO toSummaryDTO(InboundOrder order) {
        return new InboundOrderSummaryDTO(
                order.getId(),
                order.getDocNo(),
                order.getSupplier(),
                order.getStatus(),
                order.getItemCount(),
                order.getPlannedTotalQty(),
                order.getActualTotalQty(),
                order.getTransferStatus(),
                order.getRemark(),
                order.getCreatedBy(),
                order.getUpdatedBy(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private List<InboundOrderDetailDTO> toDetailDTOs(List<InboundOrderDetail> details) {
        return details.stream()
                .map(detail -> new InboundOrderDetailDTO(
                        detail.getId(),
                        detail.getLineNo(),
                        detail.getSupplierCode(),
                        detail.getSupplierName(),
                        detail.getMaterialCode(),
                        detail.getMaterialName(),
                        detail.getPackageModel(),
                        detail.getPackagingCapacity(),
                        detail.getPlannedQty(),
                        detail.getActualQty(),
                        Math.max(safeInt(detail.getPlannedQty()) - safeInt(detail.getActualQty()), 0),
                        detail.getPackageCount(),
                        calculateBoxCount(detail.getPlannedQty(), detail.getPackagingCapacity()),
                        detail.getWarehouseArea(),
                        detail.getTransferStatus(),
                        detail.getRemark()
                ))
                .collect(Collectors.toList());
    }

    private List<InventoryStockDTO> toInventoryStockDTOs(InboundOrder order, List<InboundOrderDetail> details) {
        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 按物料号+供应商去重后，查出所有库区的库存行
        List<InventoryStock> stocks = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (InboundOrderDetail detail : details) {
            String key = detail.getSupplierName() + "::" + detail.getMaterialCode();
            if (!seenKeys.add(key)) {
                continue;
            }
            stocks.addAll(inventoryStockRepository.findAllByMaterialCodeAndSupplier(
                    detail.getMaterialCode(), detail.getSupplierName()));
        }

        return stocks.stream()
                .map(stock -> {
                    int sealedQty = calculateSealedQtyByMaterial(stock.getMaterialCode(), stock.getSupplier());
                    int availableQty = Math.max(safeInt(stock.getOnHandQty()) - sealedQty, 0);
                    return new InventoryStockDTO(
                            stock.getMaterialCode(),
                            stock.getMaterialName(),
                            stock.getSupplier(),
                            availableQty,
                            stock.getLastInboundDocNo(),
                            stock.getLastInboundAt(),
                            stock.getTransferStatus(),
                            stock.getWarehouseArea()
                    );
                })
                .collect(Collectors.toList());
    }

    private InboundKanbanLabel findLabelByScanValue(String kanbanNoOrPayload) {
        String kanbanNo = parseKanbanNo(kanbanNoOrPayload);
        return inboundKanbanLabelRepository.findByKanbanNo(kanbanNo)
                .orElseThrow(() -> new EntityNotFoundException("看板不存在"));
    }

    private String parseKanbanNo(String kanbanNoOrPayload) {
        String value = trimToNull(kanbanNoOrPayload);
        if (value == null) {
            throw new IllegalArgumentException("看板号不能为空");
        }
        if (value.startsWith(QR_PREFIX)) {
            return value.substring(QR_PREFIX.length()).trim();
        }
        return value;
    }

    private List<InboundKanbanLabelDTO> toKanbanLabelDTOs(List<InboundKanbanLabel> labels) {
        return labels.stream().map(this::toKanbanLabelDTO).collect(Collectors.toList());
    }

    private InboundKanbanLabelDTO toKanbanLabelDTO(InboundKanbanLabel label) {
        return new InboundKanbanLabelDTO(
                label.getId(),
                label.getInboundOrderId(),
                label.getInboundOrderDetailId(),
                label.getDocNo(),
                label.getKanbanNo(),
                label.getQrPayload(),
                label.getMaterialCode(),
                label.getMaterialName(),
                label.getSupplierCode(),
                label.getSupplierName(),
                label.getPackageModel(),
                label.getWarehouseArea(),
                label.getLabelQty(),
                label.getPackageSeq(),
                label.getPackageTotal(),
                label.getTransferStatus(),
                label.getLabelStatus(),
                label.getPrintedAt(),
                label.getReceivedAt(),
                label.getReceivedBy(),
                label.getSealed(),
                label.getSealedAt(),
                label.getSealedBy()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String normalizeOperator(String operator) {
        return trimToNull(operator) == null ? "system" : operator.trim();
    }
}
