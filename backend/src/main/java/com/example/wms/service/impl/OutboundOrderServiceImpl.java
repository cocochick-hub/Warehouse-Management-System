package com.example.wms.service.impl;

import com.example.wms.dto.inbound.InventoryStockDTO;
import com.example.wms.dto.outbound.*;
import com.example.wms.entity.*;
import com.example.wms.repository.*;
import com.example.wms.service.OutboundOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OutboundOrderServiceImpl implements OutboundOrderService {

    private static final String STATUS_PENDING = "待出库";
    private static final String STATUS_PARTIAL = "部分完成";
    private static final String STATUS_COMPLETED = "已完成";
    private static final String STATUS_RETURNED = "已退库";
    private static final String LABEL_STATUS_RECEIVED = "已入库";
    private static final String TRANSFER_STATUS_ISSUED = "已出库";
    private static final DateTimeFormatter DOC_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger DOC_NO_SEQUENCE = new AtomicInteger(0);

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderDetailRepository outboundOrderDetailRepository;
    private final OutboundHistoryRepository outboundHistoryRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderDetailRepository inboundOrderDetailRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InboundKanbanLabelRepository inboundKanbanLabelRepository;

    public OutboundOrderServiceImpl(OutboundOrderRepository outboundOrderRepository,
                                    OutboundOrderDetailRepository outboundOrderDetailRepository,
                                    OutboundHistoryRepository outboundHistoryRepository,
                                    InboundOrderRepository inboundOrderRepository,
                                    InboundOrderDetailRepository inboundOrderDetailRepository,
                                    InventoryStockRepository inventoryStockRepository,
                                    InboundKanbanLabelRepository inboundKanbanLabelRepository) {
        this.outboundOrderRepository = outboundOrderRepository;
        this.outboundOrderDetailRepository = outboundOrderDetailRepository;
        this.outboundHistoryRepository = outboundHistoryRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.inboundOrderDetailRepository = inboundOrderDetailRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.inboundKanbanLabelRepository = inboundKanbanLabelRepository;
    }

    @Override
    public OutboundOrderPageResponse listOrders(String docNo, String supplier, String status, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<OutboundOrder> specification = buildOrderSpecification(docNo, supplier, status);
        Page<OutboundOrder> resultPage = outboundOrderRepository.findAll(specification, pageable);
        List<OutboundOrderSummaryDTO> records = resultPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());

        return new OutboundOrderPageResponse((int) resultPage.getTotalElements(), safePage, safeSize, records);
    }

    @Override
    public OutboundOrderDetailResponse getOrderDetail(Long id) {
        OutboundOrder order = findOrder(id);
        List<OutboundOrderDetail> details = outboundOrderDetailRepository.findByOutboundOrderIdOrderByLineNoAsc(id);
        return new OutboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details), toInventoryStockDTOs(details));
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse createOrder(OutboundOrderCreateRequest request, String operator) {
        validateCreateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        String docNo = generateDocNo();

        OutboundOrder order = new OutboundOrder();
        order.setDocNo(docNo);
        order.setSupplier(resolveOrderSupplier(request));
        order.setStatus(STATUS_PENDING);
        order.setItemCount(request.getDetails().size());
        order.setPlannedTotalQty(sumPlannedQty(request.getDetails()));
        order.setActualTotalQty(0);
        order.setOutboundType(defaultIfBlank(request.getOutboundType(), "带单出库"));
        order.setRemark(trimToNull(request.getRemark()));
        order.setCreatedBy(currentOperator);
        order.setUpdatedBy(currentOperator);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        OutboundOrder savedOrder = outboundOrderRepository.saveAndFlush(order);

        List<OutboundOrderDetail> details = new ArrayList<>();
        int lineNo = 1;
        for (OutboundCreateDetailRequest item : request.getDetails()) {
            OutboundOrderDetail detail = new OutboundOrderDetail();
            detail.setOutboundOrderId(savedOrder.getId());
            detail.setDocNo(savedOrder.getDocNo());
            detail.setLineNo(lineNo++);
            detail.setSupplierCode(item.getSupplierCode().trim());
            detail.setSupplierName(item.getSupplierName().trim());
            detail.setMaterialCode(item.getMaterialCode().trim());
            detail.setMaterialName(item.getMaterialName().trim());
            detail.setPlannedQty(item.getPlannedQty());
            detail.setActualQty(0);
            detail.setWarehouseArea(defaultIfBlank(item.getWarehouseArea(), "默认库区"));
            detail.setRemark(trimToNull(item.getRemark()));
            detail.setCreatedBy(currentOperator);
            detail.setUpdatedBy(currentOperator);
            detail.setCreatedAt(now);
            detail.setUpdatedAt(now);
            details.add(detail);
        }

        outboundOrderDetailRepository.saveAll(details);
        return getOrderDetail(savedOrder.getId());
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse issueOrder(Long id, OutboundIssueRequest request, String operator) {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("出库明细不能为空");
        }

        OutboundOrder order = findOrder(id);
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("已完成单据不允许再次出库");
        }

        List<OutboundOrderDetail> details = outboundOrderDetailRepository.findByOutboundOrderIdOrderByLineNoAsc(id);
        if (details.isEmpty()) {
            throw new IllegalStateException("当前出库单缺少明细，无法执行出库");
        }

        Map<Long, OutboundIssueDetailRequest> requestMap = new HashMap<>();
        for (OutboundIssueDetailRequest item : request.getDetails()) {
            if (requestMap.put(item.getDetailId(), item) != null) {
                throw new IllegalArgumentException("同一明细不能重复提交出库数量");
            }
        }

        Set<Long> detailIds = details.stream().map(OutboundOrderDetail::getId).collect(Collectors.toSet());
        for (Long detailId : requestMap.keySet()) {
            if (!detailIds.contains(detailId)) {
                throw new IllegalArgumentException("存在不属于当前出库单的明细");
            }
        }

        int totalIssueQty = requestMap.values().stream()
                .map(OutboundIssueDetailRequest::getIssueQty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (totalIssueQty <= 0) {
            throw new IllegalArgumentException("至少需要一条明细的出库数量大于0");
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        List<OutboundHistory> historyRecords = new ArrayList<>();

        for (OutboundOrderDetail detail : details) {
            OutboundIssueDetailRequest item = requestMap.get(detail.getId());
            if (item == null || item.getIssueQty() == null || item.getIssueQty() <= 0) {
                continue;
            }

            int issueQty = item.getIssueQty();
            int nextActualQty = safeInt(detail.getActualQty()) + issueQty;
            if (nextActualQty > safeInt(detail.getPlannedQty())) {
                throw new IllegalArgumentException(
                        "物料 " + detail.getMaterialCode() + " 的累计实发数量不能大于计划数量");
            }

            List<OutboundHistory> allocated = allocateFIFO(order, detail, issueQty, currentOperator, now);
            historyRecords.addAll(allocated);

            detail.setActualQty(nextActualQty);
            detail.setUpdatedBy(currentOperator);
            detail.setUpdatedAt(now);
        }

        outboundOrderDetailRepository.saveAll(details);
        if (!historyRecords.isEmpty()) {
            outboundHistoryRepository.saveAll(historyRecords);
        }

        order.setItemCount(details.size());
        order.setPlannedTotalQty(details.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum());
        order.setActualTotalQty(details.stream().mapToInt(item -> safeInt(item.getActualQty())).sum());
        order.setStatus(calculateStatus(order.getPlannedTotalQty(), order.getActualTotalQty()));
        order.setUpdatedBy(currentOperator);
        order.setUpdatedAt(now);
        outboundOrderRepository.save(order);

        return new OutboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details), toInventoryStockDTOs(details));
    }

    @Override
    public Page<OutboundHistoryDTO> listHistory(String docNo, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<OutboundHistory> specification = buildHistorySpecification(docNo);
        Page<OutboundHistory> resultPage = outboundHistoryRepository.findAll(specification, pageable);
        return resultPage.map(this::toHistoryDTO);
    }

    @Override
    public OutboundScanLabelResponse getOutboundScanLabel(String kanbanNo) {
        InboundKanbanLabel label = findKanbanLabel(kanbanNo);

        InboundOrderDetail detail = inboundOrderDetailRepository.findById(label.getInboundOrderDetailId())
                .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));

        InboundOrder inboundOrder = inboundOrderRepository.findById(label.getInboundOrderId())
                .orElseThrow(() -> new EntityNotFoundException("入库单不存在"));

        List<OutboundHistory> consumed = outboundHistoryRepository.findBySourceDetailId(label.getInboundOrderDetailId());
        int consumedQty = consumed.stream()
                .filter(h -> !STATUS_RETURNED.equals(h.getStatus()))
                .mapToInt(h -> safeInt(h.getIssueQty())).sum();
        int availableQty = Math.max(safeInt(detail.getActualQty()) - consumedQty, 0);

        boolean fifoWarning = false;
        String fifoMessage = null;
        String earliestDocNo = null;

        List<InboundOrderDetail> allDetailsForMaterial = inboundOrderDetailRepository
                .findByMaterialCodeAndSupplierName(detail.getMaterialCode(), detail.getSupplierName());

        LocalDateTime earliestCreatedAt = null;

        for (InboundOrderDetail otherDetail : allDetailsForMaterial) {
            InboundOrder otherOrder = inboundOrderRepository.findByDocNo(otherDetail.getDocNo()).orElse(null);
            if (otherOrder == null) {
                continue;
            }
            if (!STATUS_COMPLETED.equals(otherOrder.getStatus())
                    && !STATUS_PARTIAL.equals(otherOrder.getStatus())) {
                continue;
            }

            LocalDateTime orderCreatedAt = otherOrder.getCreatedAt();
            if (orderCreatedAt != null
                    && (earliestCreatedAt == null || orderCreatedAt.isBefore(earliestCreatedAt))) {
                earliestCreatedAt = orderCreatedAt;
                earliestDocNo = otherOrder.getDocNo();
            }
        }

        if (earliestDocNo != null && !earliestDocNo.equals(inboundOrder.getDocNo())) {
            fifoWarning = true;
            fifoMessage = "当前出库的库存并非最早入库，是否要继续出库？";
        }

        boolean sealed = Boolean.TRUE.equals(label.getSealed());
        String sealedMessage = sealed ? "该看板已封存，无法出库" : null;

        return new OutboundScanLabelResponse(
                label.getId(),
                label.getKanbanNo(),
                label.getDocNo(),
                label.getMaterialCode(),
                label.getMaterialName(),
                label.getSupplierCode(),
                label.getSupplierName(),
                label.getLabelQty(),
                label.getWarehouseArea(),
                label.getLabelStatus(),
                label.getInboundOrderId(),
                label.getInboundOrderDetailId(),
                fifoWarning,
                fifoMessage,
                earliestDocNo,
                availableQty,
                sealed,
                sealedMessage
        );
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse issueByScan(OutboundScanIssueRequest request, String operator) {
        InboundKanbanLabel label = findKanbanLabel(request.getKanbanNo());
        if (!LABEL_STATUS_RECEIVED.equals(label.getLabelStatus())) {
            throw new IllegalStateException("该看板尚未完成入库，无法出库");
        }
        // 防止同一看板号重复出库
        if (TRANSFER_STATUS_ISSUED.equals(label.getTransferStatus())) {
            throw new IllegalStateException("该看板已出库，无法重复出库");
        }
        if (Boolean.TRUE.equals(label.getSealed())) {
            throw new IllegalStateException("该看板已被封存，无法出库，请先解封");
        }
        if ("已转包".equals(label.getTransferStatus())) {
            throw new IllegalStateException("该看板已全量转包，无法出库");
        }
        if ("转包".equals(label.getTransferStatus())) {
            throw new IllegalStateException("该看板已被部分转包，不允许直接出库");
        }

        OutboundOrder order;
        if (request.getOutboundOrderId() != null) {
            order = findOrder(request.getOutboundOrderId());
        } else if (trimToNull(request.getOutboundDocNo()) != null) {
            order = outboundOrderRepository.findByDocNo(request.getOutboundDocNo().trim())
                    .orElseThrow(() -> new EntityNotFoundException("出库单不存在"));
        } else {
            throw new IllegalArgumentException("出库单ID或出库单号不能为空");
        }
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("已完成单据不允许再次出库");
        }

        InboundOrderDetail inboundDetail = inboundOrderDetailRepository
                .findById(label.getInboundOrderDetailId())
                .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));

        List<OutboundHistory> consumed = outboundHistoryRepository.findBySourceDetailId(inboundDetail.getId());
        int consumedQty = consumed.stream()
                .filter(h -> !STATUS_RETURNED.equals(h.getStatus()))
                .mapToInt(h -> safeInt(h.getIssueQty())).sum();
        int availableQty = Math.max(safeInt(inboundDetail.getActualQty()) - consumedQty, 0);

        int issueQty = safeInt(request.getIssueQty());
        if (issueQty <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }
        if (issueQty > availableQty) {
            throw new IllegalStateException(
                    "库存不足，当前看板可用 " + availableQty + "，需要 " + issueQty);
        }

        List<OutboundOrderDetail> details = outboundOrderDetailRepository
                .findByOutboundOrderIdOrderByLineNoAsc(order.getId());

        OutboundOrderDetail matchingDetail = null;
        for (OutboundOrderDetail detail : details) {
            if (label.getMaterialCode().equals(detail.getMaterialCode())
                    && label.getSupplierName().equals(detail.getSupplierName())) {
                matchingDetail = detail;
                break;
            }
        }
        if (matchingDetail == null) {
            throw new IllegalArgumentException("出库单中未找到与看板匹配的明细行（物料：" + label.getMaterialCode() + "，需求方：" + label.getSupplierName() + "）");
        }

        int nextActualQty = safeInt(matchingDetail.getActualQty()) + issueQty;
        if (nextActualQty > safeInt(matchingDetail.getPlannedQty())) {
            throw new IllegalArgumentException("物料 " + matchingDetail.getMaterialCode() + " 的累计实发数量不能大于计划数量");
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        String warehouseArea = defaultIfBlank(request.getWarehouseArea(), defaultIfBlank(matchingDetail.getWarehouseArea(), "默认库区"));

        OutboundHistory history = new OutboundHistory();
        history.setOutboundOrderId(order.getId());
        history.setOutboundDetailId(matchingDetail.getId());
        history.setDocNo(order.getDocNo());
        history.setMaterialCode(matchingDetail.getMaterialCode());
        history.setMaterialName(matchingDetail.getMaterialName());
        history.setSupplierName(matchingDetail.getSupplierName());
        history.setIssueQty(issueQty);
        history.setSourceInboundDoc(inboundDetail.getDocNo());
        history.setSourceDetailId(inboundDetail.getId());
        history.setKanbanLabelId(label.getId());
        history.setWarehouseArea(warehouseArea);
        history.setIssuedBy(currentOperator);
        history.setStatus("已出库");
        history.setCreatedAt(now);
        outboundHistoryRepository.save(history);

        matchingDetail.setActualQty(nextActualQty);
        matchingDetail.setUpdatedBy(currentOperator);
        matchingDetail.setUpdatedAt(now);
        outboundOrderDetailRepository.save(matchingDetail);

        // 按看板库区定位库存行扣减（扫码出库知道物料具体在哪个库区）
        String stockArea = defaultIfBlank(label.getWarehouseArea(), "默认库区");
        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplierAndWarehouseArea(matchingDetail.getMaterialCode(), matchingDetail.getSupplierName(), stockArea)
                .orElse(null);
        if (stock == null) {
            throw new IllegalStateException(
                    "物料 " + matchingDetail.getMaterialCode() + "（需求方 " + matchingDetail.getSupplierName()
                    + "）在库区 " + stockArea + " 不存在库存记录，请先入库");
        }
        int onHand = safeInt(stock.getOnHandQty());
        if (onHand < issueQty) {
            throw new IllegalStateException(
                    "物料 " + matchingDetail.getMaterialCode() + "（库区 " + stockArea + "）当前库存 " + onHand + "，不足 " + issueQty);
        }
        stock.setOnHandQty(onHand - issueQty);
        stock.setUpdatedBy(currentOperator);
        stock.setUpdatedAt(now);
        inventoryStockRepository.save(stock);

        order.setItemCount(order.getItemCount());
        order.setPlannedTotalQty(order.getPlannedTotalQty());
        order.setActualTotalQty(details.stream().mapToInt(item -> safeInt(item.getActualQty())).sum());
        order.setStatus(calculateStatus(order.getPlannedTotalQty(), order.getActualTotalQty()));
        order.setUpdatedBy(currentOperator);
        order.setUpdatedAt(now);
        outboundOrderRepository.save(order);

        // 更新看板标签状态，防止重复出库
        label.setTransferStatus(TRANSFER_STATUS_ISSUED);
        inboundKanbanLabelRepository.save(label);

        details = outboundOrderDetailRepository.findByOutboundOrderIdOrderByLineNoAsc(order.getId());
        return new OutboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details), toInventoryStockDTOs(details));
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse issueWithoutOrder(OutboundOrderlessRequest request, String operator) {
        String materialCode = trimToNull(request.getMaterialCode());
        String materialName = trimToNull(request.getMaterialName());
        String supplierCode = trimToNull(request.getSupplierCode());
        String supplierName = trimToNull(request.getSupplierName());
        int issueQty = safeInt(request.getIssueQty());

        if (materialCode == null || materialName == null || supplierCode == null || supplierName == null) {
            throw new IllegalArgumentException("物料代码、物料名称、需求方代码、需求方名称不能为空");
        }
        if (issueQty <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        String docNo = generateDocNo();

        OutboundOrder order = new OutboundOrder();
        order.setDocNo(docNo);
        order.setSupplier(supplierName);
        order.setStatus(STATUS_PENDING);
        order.setItemCount(1);
        order.setPlannedTotalQty(issueQty);
        order.setActualTotalQty(0);
        order.setOutboundType("不带单出库");
        order.setRemark(trimToNull(request.getRemark()));
        order.setCreatedBy(currentOperator);
        order.setUpdatedBy(currentOperator);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        OutboundOrder savedOrder = outboundOrderRepository.saveAndFlush(order);

        OutboundOrderDetail detail = new OutboundOrderDetail();
        detail.setOutboundOrderId(savedOrder.getId());
        detail.setDocNo(savedOrder.getDocNo());
        detail.setLineNo(1);
        detail.setSupplierCode(supplierCode);
        detail.setSupplierName(supplierName);
        detail.setMaterialCode(materialCode);
        detail.setMaterialName(materialName);
        detail.setPlannedQty(issueQty);
        detail.setActualQty(0);
        detail.setWarehouseArea(defaultIfBlank(request.getWarehouseArea(), "默认库区"));
        detail.setRemark(trimToNull(request.getRemark()));
        detail.setCreatedBy(currentOperator);
        detail.setUpdatedBy(currentOperator);
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);

        outboundOrderDetailRepository.save(detail);

        List<OutboundOrderDetail> details = new ArrayList<>();
        details.add(detail);

        List<OutboundHistory> historyRecords = allocateFIFO(savedOrder, detail, issueQty, currentOperator, now);
        if (!historyRecords.isEmpty()) {
            outboundHistoryRepository.saveAll(historyRecords);
        }

        detail.setActualQty(issueQty);
        detail.setUpdatedBy(currentOperator);
        detail.setUpdatedAt(now);
        outboundOrderDetailRepository.save(detail);

        savedOrder.setPlannedTotalQty(issueQty);
        savedOrder.setActualTotalQty(issueQty);
        savedOrder.setStatus(calculateStatus(issueQty, issueQty));
        savedOrder.setUpdatedBy(currentOperator);
        savedOrder.setUpdatedAt(now);
        outboundOrderRepository.save(savedOrder);

        return new OutboundOrderDetailResponse(toSummaryDTO(savedOrder), toDetailDTOs(details), toInventoryStockDTOs(details));
    }

    // ==================== Private Helpers ====================

    private OutboundOrder findOrder(Long id) {
        return outboundOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("出库单不存在"));
    }

    private InboundKanbanLabel findKanbanLabel(String kanbanNo) {
        return inboundKanbanLabelRepository.findByKanbanNo(kanbanNo)
                .orElseThrow(() -> new EntityNotFoundException("看板不存在"));
    }

    private void validateCreateRequest(OutboundOrderCreateRequest request) {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("出库单至少包含一条明细");
        }

        Set<String> supplierMaterialKeys = new HashSet<>();
        for (OutboundCreateDetailRequest detail : request.getDetails()) {
            String supplierCode = trimToNull(detail.getSupplierCode());
            if (supplierCode == null) {
                throw new IllegalArgumentException("需求方代码不能为空");
            }
            String supplierName = trimToNull(detail.getSupplierName());
            if (supplierName == null) {
                throw new IllegalArgumentException("需求方名称不能为空");
            }
            String materialCode = trimToNull(detail.getMaterialCode());
            if (materialCode == null) {
                throw new IllegalArgumentException("物料号不能为空");
            }
            String key = supplierCode + "::" + materialCode;
            if (!supplierMaterialKeys.add(key)) {
                throw new IllegalArgumentException("同一张出库单中不允许重复选择同一需求方下的同一物料");
            }
        }
    }

    private synchronized String generateDocNo() {
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

    private int sumPlannedQty(List<OutboundCreateDetailRequest> details) {
        return details.stream().mapToInt(OutboundCreateDetailRequest::getPlannedQty).sum();
    }

    private String resolveOrderSupplier(OutboundOrderCreateRequest request) {
        String requestSupplier = trimToNull(request.getSupplier());
        if (requestSupplier != null) {
            return requestSupplier;
        }

        List<String> supplierNames = request.getDetails().stream()
                .map(OutboundCreateDetailRequest::getSupplierName)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (supplierNames.size() == 1) {
            return supplierNames.get(0);
        }
        return "多需求方";
    }

    private List<OutboundHistory> allocateFIFO(OutboundOrder order, OutboundOrderDetail detail,
                                                int issueQty, String operator, LocalDateTime now) {
        List<InboundOrderDetail> inboundDetails = inboundOrderDetailRepository
                .findByMaterialCodeAndSupplierName(detail.getMaterialCode(), detail.getSupplierName());

        List<InboundOrderDetail> eligible = new ArrayList<>();
        for (InboundOrderDetail id : inboundDetails) {
            InboundOrder inboundOrder = inboundOrderRepository.findByDocNo(id.getDocNo()).orElse(null);
            if (inboundOrder == null) {
                continue;
            }
            if (STATUS_COMPLETED.equals(inboundOrder.getStatus())
                    || STATUS_PARTIAL.equals(inboundOrder.getStatus())) {
                eligible.add(id);
            }
        }

        eligible.sort(Comparator.comparing(InboundOrderDetail::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InboundOrderDetail::getLineNo));

        int totalAvailable = 0;
        for (InboundOrderDetail id : eligible) {
            List<OutboundHistory> consumed = outboundHistoryRepository.findBySourceDetailId(id.getId());
            int consumedQty = consumed.stream()
                    .filter(h -> !STATUS_RETURNED.equals(h.getStatus()))
                    .mapToInt(h -> safeInt(h.getIssueQty())).sum();
            int sealedQty = calculateSealedQty(id.getId());
            int availableQty = safeInt(id.getActualQty()) - consumedQty - sealedQty;
            totalAvailable += Math.max(availableQty, 0);
        }

        if (totalAvailable < issueQty) {
            throw new IllegalStateException(
                    "物料 " + detail.getMaterialCode() + " 库存不足（含已封存量），需要 " + issueQty
                            + "，可用 " + totalAvailable);
        }

        List<OutboundHistory> records = new ArrayList<>();
        int remaining = issueQty;
        for (InboundOrderDetail id : eligible) {
            if (remaining <= 0) {
                break;
            }

            List<OutboundHistory> consumed = outboundHistoryRepository.findBySourceDetailId(id.getId());
            int consumedQty = consumed.stream()
                    .filter(h -> !STATUS_RETURNED.equals(h.getStatus()))
                    .mapToInt(h -> safeInt(h.getIssueQty())).sum();
            int sealedQty = calculateSealedQty(id.getId());
            int availableQty = safeInt(id.getActualQty()) - consumedQty - sealedQty;
            if (availableQty <= 0) {
                continue;
            }

            int allocateQty = Math.min(remaining, availableQty);

            OutboundHistory history = new OutboundHistory();
            history.setOutboundOrderId(order.getId());
            history.setOutboundDetailId(detail.getId());
            history.setDocNo(order.getDocNo());
            history.setMaterialCode(detail.getMaterialCode());
            history.setMaterialName(detail.getMaterialName());
            history.setSupplierName(detail.getSupplierName());
            history.setIssueQty(allocateQty);
            history.setSourceInboundDoc(id.getDocNo());
            history.setSourceDetailId(id.getId());
            history.setWarehouseArea(defaultIfBlank(detail.getWarehouseArea(), "默认库区"));
            history.setStatus("已出库");
            history.setIssuedBy(operator);
            history.setCreatedAt(now);
            records.add(history);

            remaining -= allocateQty;
        }

        // 跨库区累加校验库存总量，再逐库区扣减
        List<InventoryStock> areaStocks = inventoryStockRepository
                .findAllByMaterialCodeAndSupplier(detail.getMaterialCode(), detail.getSupplierName());
        int totalOnHand = areaStocks.stream().mapToInt(s -> safeInt(s.getOnHandQty())).sum();
        if (totalOnHand < issueQty) {
            throw new IllegalStateException(
                    "物料 " + detail.getMaterialCode() + "（需求方 " + detail.getSupplierName()
                    + "）当前总库存 " + totalOnHand + "，不足 " + issueQty);
        }
        int toDeduct = issueQty;
        for (InventoryStock areaStock : areaStocks) {
            if (toDeduct <= 0) break;
            int deduct = Math.min(safeInt(areaStock.getOnHandQty()), toDeduct);
            areaStock.setOnHandQty(safeInt(areaStock.getOnHandQty()) - deduct);
            areaStock.setUpdatedBy(operator);
            areaStock.setUpdatedAt(now);
            inventoryStockRepository.save(areaStock);
            toDeduct -= deduct;
        }

        return records;
    }

    private Specification<OutboundOrder> buildOrderSpecification(String docNo, String supplier, String status) {
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

    private Specification<OutboundHistory> buildHistorySpecification(String docNo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String docNoKeyword = trimToNull(docNo);
            if (docNoKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("docNo"), "%" + docNoKeyword + "%"));
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

    private OutboundOrderSummaryDTO toSummaryDTO(OutboundOrder order) {
        return new OutboundOrderSummaryDTO(
                order.getId(),
                order.getDocNo(),
                order.getSupplier(),
                order.getStatus(),
                order.getItemCount(),
                order.getPlannedTotalQty(),
                order.getActualTotalQty(),
                order.getOutboundType(),
                order.getRemark(),
                order.getCreatedBy(),
                order.getUpdatedBy(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private List<OutboundOrderDetailDTO> toDetailDTOs(List<OutboundOrderDetail> details) {
        return details.stream()
                .map(detail -> new OutboundOrderDetailDTO(
                        detail.getId(),
                        detail.getLineNo(),
                        detail.getSupplierCode(),
                        detail.getSupplierName(),
                        detail.getMaterialCode(),
                        detail.getMaterialName(),
                        detail.getPlannedQty(),
                        detail.getActualQty(),
                        Math.max(safeInt(detail.getPlannedQty()) - safeInt(detail.getActualQty()), 0),
                        detail.getWarehouseArea(),
                        detail.getRemark()
                ))
                .collect(Collectors.toList());
    }

    private List<InventoryStockDTO> toInventoryStockDTOs(List<OutboundOrderDetail> details) {
        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 按物料号+供应商去重后，查出所有库区的库存行
        List<InventoryStock> stocks = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (OutboundOrderDetail detail : details) {
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

    private OutboundHistoryDTO toHistoryDTO(OutboundHistory history) {
        return new OutboundHistoryDTO(
                history.getId(),
                history.getDocNo(),
                history.getMaterialCode(),
                history.getMaterialName(),
                history.getSupplierName(),
                history.getIssueQty(),
                history.getSourceInboundDoc(),
                history.getWarehouseArea(),
                history.getIssuedBy(),
                history.getCreatedAt(),
                history.getStatus()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /** 计算指定入库明细关联的已封存看板数量总和 */
    private int calculateSealedQty(Long inboundDetailId) {
        List<InboundKanbanLabel> labels = inboundKanbanLabelRepository
                .findByInboundOrderDetailIdIn(java.util.Collections.singleton(inboundDetailId));
        return labels.stream()
                .filter(label -> Boolean.TRUE.equals(label.getSealed()))
                .mapToInt(label -> safeInt(label.getLabelQty()))
                .sum();
    }

    /** 计算指定物料+供应商已封存的看板总数 */
    private int calculateSealedQtyByMaterial(String materialCode, String supplierName) {
        return inboundKanbanLabelRepository
                .findByMaterialCodeAndSupplierNameAndSealedTrue(materialCode, supplierName)
                .stream()
                .mapToInt(label -> safeInt(label.getLabelQty()))
                .sum();
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

    @Override
    public OutboundReturnLabelResponse getReturnLabel(String kanbanNo) {
        InboundKanbanLabel label;
        try {
            label = findKanbanLabel(kanbanNo);
        } catch (EntityNotFoundException e) {
            return new OutboundReturnLabelResponse(
                    kanbanNo, null, null, null, null, null, null, null,
                    false, "看板号不存在");
        }

        // 优先通过看板ID精确查找出库历史
        List<OutboundHistory> histories = outboundHistoryRepository
                .findByKanbanLabelId(label.getId());

        // 兼容旧版出库（allocateFIFO 无 kanbanLabelId）：回退到按入库明细查找
        if (histories.isEmpty()) {
            InboundOrderDetail inboundDetail = inboundOrderDetailRepository
                    .findById(label.getInboundOrderDetailId())
                    .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));
            histories = outboundHistoryRepository
                    .findBySourceDetailId(inboundDetail.getId());
        }

        if (histories.isEmpty()) {
            return new OutboundReturnLabelResponse(
                    kanbanNo, label.getMaterialCode(), label.getMaterialName(),
                    label.getSupplierName(), null, null, null, label.getWarehouseArea(),
                    false, "该看板对应的物料未被出库过");
        }

        // 取最近一条出库记录（按时间倒序第一条）
        OutboundHistory latestHistory = histories.stream()
                .filter(h -> h.getCreatedAt() != null)
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(histories.get(0));

        // 检查是否已退库
        if (STATUS_RETURNED.equals(latestHistory.getStatus())) {
            return new OutboundReturnLabelResponse(
                    kanbanNo, label.getMaterialCode(), label.getMaterialName(),
                    label.getSupplierName(), latestHistory.getDocNo(),
                    latestHistory.getIssueQty(),
                    latestHistory.getCreatedAt() != null ? latestHistory.getCreatedAt().toString() : null,
                    label.getWarehouseArea(),
                    false, "该看板已退库，无法重复退库");
        }

        return new OutboundReturnLabelResponse(
                kanbanNo, label.getMaterialCode(), label.getMaterialName(),
                label.getSupplierName(), latestHistory.getDocNo(),
                latestHistory.getIssueQty(),
                latestHistory.getCreatedAt() != null ? latestHistory.getCreatedAt().toString() : null,
                label.getWarehouseArea(),
                true, null);
    }

    @Override
    @Transactional
    public OutboundReturnResponse returnByScan(OutboundReturnRequest request, String operator) {
        InboundKanbanLabel label = findKanbanLabel(request.getKanbanNo());

        // 优先通过看板ID精确查找出库历史
        List<OutboundHistory> histories = outboundHistoryRepository
                .findByKanbanLabelId(label.getId());

        // 兼容旧版出库（allocateFIFO 无 kanbanLabelId）：回退到按入库明细查找
        if (histories.isEmpty()) {
            InboundOrderDetail inboundDetail = inboundOrderDetailRepository
                    .findById(label.getInboundOrderDetailId())
                    .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));
            histories = outboundHistoryRepository
                    .findBySourceDetailId(inboundDetail.getId());
        }

        OutboundHistory latestHistory = null;
        for (OutboundHistory h : histories) {
            if (!STATUS_RETURNED.equals(h.getStatus())) {
                latestHistory = h;
                break;
            }
        }

        if (latestHistory == null) {
            if (histories.isEmpty()) {
                throw new IllegalStateException("该看板未被出库过");
            } else {
                throw new IllegalStateException("该看板已退库，无法重复退库");
            }
        }

        int returnQty = safeInt(latestHistory.getIssueQty());

        // 1. 库存回增（按看板库区定位）
        String returnArea = defaultIfBlank(label.getWarehouseArea(), "默认库区");
        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplierAndWarehouseArea(label.getMaterialCode(), label.getSupplierName(), returnArea)
                .orElse(null);

        if (stock == null) {
            stock = new InventoryStock();
            stock.setMaterialCode(label.getMaterialCode());
            stock.setMaterialName(label.getMaterialName());
            stock.setSupplier(label.getSupplierName());
            stock.setOnHandQty(returnQty);
            stock.setLastInboundDocNo(latestHistory.getDocNo());
            stock.setLastInboundAt(LocalDateTime.now());
            stock.setWarehouseArea(label.getWarehouseArea());
            stock.setTransferStatus("不转包");
            stock.setCreatedBy(normalizeOperator(operator));
            stock.setUpdatedBy(normalizeOperator(operator));
            stock.setCreatedAt(LocalDateTime.now());
            stock.setUpdatedAt(LocalDateTime.now());
        } else {
            stock.setOnHandQty(safeInt(stock.getOnHandQty()) + returnQty);
            stock.setUpdatedBy(normalizeOperator(operator));
            stock.setUpdatedAt(LocalDateTime.now());
        }
        inventoryStockRepository.save(stock);

        // 2. 出库历史标记已退库
        latestHistory.setStatus(STATUS_RETURNED);
        outboundHistoryRepository.save(latestHistory);

        // 2.1 看板标签恢复为已入库（退库后同一看板可再次出库）
        label.setLabelStatus(LABEL_STATUS_RECEIVED);
        label.setTransferStatus(null);
        inboundKanbanLabelRepository.save(label);

        // 3. 更新出库明细实发数量（退库后扣减）
        OutboundOrderDetail outboundDetail = outboundOrderDetailRepository
                .findById(latestHistory.getOutboundDetailId()).orElse(null);
        if (outboundDetail != null) {
            int newActualQty = Math.max(safeInt(outboundDetail.getActualQty()) - returnQty, 0);
            outboundDetail.setActualQty(newActualQty);
            outboundDetail.setUpdatedBy(normalizeOperator(operator));
            outboundDetail.setUpdatedAt(LocalDateTime.now());
            outboundOrderDetailRepository.save(outboundDetail);
        }

        // 4. 重新计算出库单状态
        OutboundOrder order = outboundOrderRepository.findById(latestHistory.getOutboundOrderId()).orElse(null);
        if (order != null) {
            List<OutboundOrderDetail> orderDetails = outboundOrderDetailRepository
                    .findByOutboundOrderIdOrderByLineNoAsc(order.getId());
            int plannedTotal = orderDetails.stream().mapToInt(d -> safeInt(d.getPlannedQty())).sum();
            int actualTotal = orderDetails.stream().mapToInt(d -> safeInt(d.getActualQty())).sum();

            // 判断是否全部退库
            boolean allReturned = true;
            for (OutboundOrderDetail detail : orderDetails) {
                List<OutboundHistory> detailHistories = outboundHistoryRepository
                        .findByOutboundDetailId(detail.getId());
                boolean thisLineAllReturned = detailHistories.stream()
                        .allMatch(h -> STATUS_RETURNED.equals(h.getStatus()));
                if (!thisLineAllReturned) {
                    allReturned = false;
                    break;
                }
            }

            order.setPlannedTotalQty(plannedTotal);
            order.setActualTotalQty(actualTotal);
            if (allReturned) {
                order.setStatus(STATUS_RETURNED);
            } else {
                order.setStatus(calculateStatus(plannedTotal, actualTotal));
                // 退库后出库单恢复为可操作状态，允许继续出库
            }
            order.setUpdatedBy(normalizeOperator(operator));
            order.setUpdatedAt(LocalDateTime.now());
            outboundOrderRepository.save(order);
        }

        return new OutboundReturnResponse(
                latestHistory.getDocNo(),
                label.getMaterialCode(),
                returnQty,
                safeInt(stock.getOnHandQty()));
    }

    @Override
    public List<OutboundKanbanLabelDTO> getAvailableKanbanLabels(Long orderId) {
        OutboundOrder order = findOrder(orderId);
        List<OutboundOrderDetail> details = outboundOrderDetailRepository
                .findByOutboundOrderIdOrderByLineNoAsc(orderId);
        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 对每个明细行，按 (materialCode, supplierName) 查可用看板
        List<OutboundKanbanLabelDTO> result = new ArrayList<>();
        Set<Long> seenLabelIds = new HashSet<>();

        for (OutboundOrderDetail detail : details) {
            List<InboundKanbanLabel> labels = inboundKanbanLabelRepository
                    .findByMaterialCodeAndSupplierNameOrderByCreatedAtAsc(
                            detail.getMaterialCode(), detail.getSupplierName());

            for (InboundKanbanLabel label : labels) {
                // 只显示已入库、未封存、未出库/转包的看板
                if (!LABEL_STATUS_RECEIVED.equals(label.getLabelStatus())) {
                    continue;
                }
                if (Boolean.TRUE.equals(label.getSealed())) {
                    continue;
                }
                if (TRANSFER_STATUS_ISSUED.equals(label.getTransferStatus())
                        || "已转包".equals(label.getTransferStatus())
                        || "转包".equals(label.getTransferStatus())) {
                    continue;
                }
                // 去重（同一看板可能被多条明细匹配到，取第一条）
                if (!seenLabelIds.add(label.getId())) {
                    continue;
                }

                // 计算可用数量
                List<OutboundHistory> consumed = outboundHistoryRepository
                        .findBySourceDetailId(label.getInboundOrderDetailId());
                int consumedQty = consumed.stream()
                        .filter(h -> !STATUS_RETURNED.equals(h.getStatus()))
                        .mapToInt(h -> safeInt(h.getIssueQty())).sum();
                int sealedQty = calculateSealedQty(label.getInboundOrderDetailId());
                InboundOrderDetail inboundDetail = inboundOrderDetailRepository
                        .findById(label.getInboundOrderDetailId()).orElse(null);
                int inboundActual = inboundDetail != null ? safeInt(inboundDetail.getActualQty()) : 0;
                int availableQty = Math.max(inboundActual - consumedQty - sealedQty, 0);

                int pendingQty = Math.max(safeInt(detail.getPlannedQty()) - safeInt(detail.getActualQty()), 0);

                OutboundKanbanLabelDTO dto = new OutboundKanbanLabelDTO();
                dto.setId(label.getId());
                dto.setKanbanNo(label.getKanbanNo());
                dto.setDocNo(label.getDocNo());
                dto.setMaterialCode(label.getMaterialCode());
                dto.setMaterialName(label.getMaterialName());
                dto.setSupplierCode(label.getSupplierCode());
                dto.setSupplierName(label.getSupplierName());
                dto.setLabelQty(safeInt(label.getLabelQty()));
                dto.setAvailableQty(availableQty);
                dto.setPackageSeq(label.getPackageSeq());
                dto.setPackageTotal(label.getPackageTotal());
                dto.setWarehouseArea(label.getWarehouseArea());
                dto.setLabelStatus(label.getLabelStatus());
                dto.setTransferStatus(label.getTransferStatus());
                dto.setInboundOrderId(label.getInboundOrderId());
                dto.setInboundOrderDetailId(label.getInboundOrderDetailId());
                dto.setSealed(label.getSealed());
                dto.setCreatedAt(label.getCreatedAt());
                dto.setMatchedOutboundDetailId(detail.getId());
                dto.setMatchedDetailInfo(detail.getMaterialCode()
                        + " plannedQty=" + safeInt(detail.getPlannedQty())
                        + " actualQty=" + safeInt(detail.getActualQty())
                        + " pendingQty=" + pendingQty);
                result.add(dto);
            }
        }

        // 按入库明细创建时间 ASC 排序（FIFO），再按 packageSeq ASC
        result.sort(Comparator.comparing(OutboundKanbanLabelDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(dto -> dto.getPackageSeq() != null ? dto.getPackageSeq() : 0));

        return result;
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse issueByLabels(Long orderId, List<Long> labelIds, Map<Long, Integer> labelIssueQtys, String operator) {
        if (labelIds == null || labelIds.isEmpty()) {
            throw new IllegalArgumentException("出库看板不能为空");
        }

        // 1. 查出库单，验证状态
        OutboundOrder order = findOrder(orderId);
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("已完成单据不允许再次出库");
        }

        // 2. 查出库单所有明细行
        List<OutboundOrderDetail> details = outboundOrderDetailRepository
                .findByOutboundOrderIdOrderByLineNoAsc(orderId);
        if (details.isEmpty()) {
            throw new IllegalStateException("当前出库单缺少明细，无法执行出库");
        }

        // 3. 查所有看板并验证
        List<InboundKanbanLabel> labels = inboundKanbanLabelRepository.findAllById(labelIds);
        if (labels.size() != labelIds.size()) {
            throw new IllegalArgumentException("存在无效的看板ID");
        }

        for (InboundKanbanLabel label : labels) {
            if (!LABEL_STATUS_RECEIVED.equals(label.getLabelStatus())) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 尚未完成入库，无法出库");
            }
            if (TRANSFER_STATUS_ISSUED.equals(label.getTransferStatus())) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 已出库，无法重复出库");
            }
            if (Boolean.TRUE.equals(label.getSealed())) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 已被封存，无法出库");
            }
            if ("已转包".equals(label.getTransferStatus())) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 已全量转包，无法出库");
            }
            if ("转包".equals(label.getTransferStatus())) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 已被部分转包，不允许直接出库");
            }
        }

        // 4. 按 (materialCode, supplierName) 匹配看板到出库明细行
        Map<Long, OutboundOrderDetail> detailMap = new HashMap<>();
        Map<Long, List<InboundKanbanLabel>> detailLabelsMap = new HashMap<>();
        for (InboundKanbanLabel label : labels) {
            OutboundOrderDetail matched = null;
            for (OutboundOrderDetail detail : details) {
                if (label.getMaterialCode().equals(detail.getMaterialCode())
                        && label.getSupplierName().equals(detail.getSupplierName())) {
                    matched = detail;
                    break;
                }
            }
            if (matched == null) {
                throw new IllegalArgumentException(
                        "出库单中未找到与看板 " + label.getKanbanNo()
                        + "（物料：" + label.getMaterialCode()
                        + "，需求方：" + label.getSupplierName() + "）匹配的明细行");
            }
            detailMap.putIfAbsent(matched.getId(), matched);
            detailLabelsMap.computeIfAbsent(matched.getId(), k -> new ArrayList<>()).add(label);
        }

        // 5. 校验每个明细行的选中看板总数量不超过待出库数量
        for (Map.Entry<Long, List<InboundKanbanLabel>> entry : detailLabelsMap.entrySet()) {
            OutboundOrderDetail detail = detailMap.get(entry.getKey());
            int totalSelectedQty = entry.getValue().stream()
                    .mapToInt(l -> {
                        Integer override = labelIssueQtys != null ? labelIssueQtys.get(l.getId()) : null;
                        return override != null ? override : safeInt(l.getLabelQty());
                    }).sum();
            int pendingQty = safeInt(detail.getPlannedQty()) - safeInt(detail.getActualQty());
            if (totalSelectedQty > pendingQty) {
                throw new IllegalArgumentException(
                        "物料 " + detail.getMaterialCode() + " 的本次出库数 " + totalSelectedQty
                        + " 超过待出库数 " + pendingQty);
            }
        }

        // 6. 验证库存总量充足（跨库区）
        for (Map.Entry<Long, List<InboundKanbanLabel>> entry : detailLabelsMap.entrySet()) {
            OutboundOrderDetail detail = detailMap.get(entry.getKey());
            int totalIssueQty = entry.getValue().stream()
                    .mapToInt(l -> {
                        Integer override = labelIssueQtys != null ? labelIssueQtys.get(l.getId()) : null;
                        return override != null ? override : safeInt(l.getLabelQty());
                    }).sum();
            List<InventoryStock> areaStocks = inventoryStockRepository
                    .findAllByMaterialCodeAndSupplier(detail.getMaterialCode(), detail.getSupplierName());
            int totalOnHand = areaStocks.stream().mapToInt(s -> safeInt(s.getOnHandQty())).sum();
            if (totalOnHand < totalIssueQty) {
                throw new IllegalStateException(
                        "物料 " + detail.getMaterialCode() + "（需求方 " + detail.getSupplierName()
                        + "）当前总库存 " + totalOnHand + "，不足 " + totalIssueQty);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);

        // 7. 为每个看板创建出库历史记录
        List<OutboundHistory> historyRecords = new ArrayList<>();
        for (InboundKanbanLabel label : labels) {
            OutboundOrderDetail detail = detailMap.get(
                    detailLabelsMap.entrySet().stream()
                            .filter(e -> e.getValue().contains(label))
                            .findFirst().get().getKey());

            Integer override = labelIssueQtys != null ? labelIssueQtys.get(label.getId()) : null;
            int issueQty = override != null ? override : safeInt(label.getLabelQty());
            String warehouseArea = defaultIfBlank(label.getWarehouseArea(),
                    defaultIfBlank(detail.getWarehouseArea(), "默认库区"));

            OutboundHistory history = new OutboundHistory();
            history.setOutboundOrderId(order.getId());
            history.setOutboundDetailId(detail.getId());
            history.setDocNo(order.getDocNo());
            history.setMaterialCode(detail.getMaterialCode());
            history.setMaterialName(detail.getMaterialName());
            history.setSupplierName(detail.getSupplierName());
            history.setIssueQty(issueQty);
            history.setSourceInboundDoc(label.getDocNo());
            history.setSourceDetailId(label.getInboundOrderDetailId());
            history.setKanbanLabelId(label.getId());
            history.setWarehouseArea(warehouseArea);
            history.setIssuedBy(currentOperator);
            history.setStatus("已出库");
            history.setCreatedAt(now);
            historyRecords.add(history);
        }
        outboundHistoryRepository.saveAll(historyRecords);

        // 8. 按看板库区扣减库存
        for (Map.Entry<Long, List<InboundKanbanLabel>> entry : detailLabelsMap.entrySet()) {
            OutboundOrderDetail detail = detailMap.get(entry.getKey());
            int totalIssueQty = entry.getValue().stream()
                    .mapToInt(l -> {
                        Integer override = labelIssueQtys != null ? labelIssueQtys.get(l.getId()) : null;
                        return override != null ? override : safeInt(l.getLabelQty());
                    }).sum();

            List<InventoryStock> areaStocks = inventoryStockRepository
                    .findAllByMaterialCodeAndSupplier(detail.getMaterialCode(), detail.getSupplierName());
            int toDeduct = totalIssueQty;
            for (InventoryStock areaStock : areaStocks) {
                if (toDeduct <= 0) break;
                int deduct = Math.min(safeInt(areaStock.getOnHandQty()), toDeduct);
                areaStock.setOnHandQty(safeInt(areaStock.getOnHandQty()) - deduct);
                areaStock.setUpdatedBy(currentOperator);
                areaStock.setUpdatedAt(now);
                inventoryStockRepository.save(areaStock);
                toDeduct -= deduct;
            }
        }

        // 9. 标记看板状态（部分出库仅扣减可用数量，不标记已出库）
        for (InboundKanbanLabel label : labels) {
            Integer override = labelIssueQtys != null ? labelIssueQtys.get(label.getId()) : null;
            if (override == null || override >= safeInt(label.getLabelQty())) {
                // 全量出库或未指定数量，标记为已出库
                label.setTransferStatus(TRANSFER_STATUS_ISSUED);
            } else {
                // 部分出库，仅扣减标签可用数量（labelQty 改为剩余数量）
                label.setLabelQty(safeInt(label.getLabelQty()) - override);
            }
        }
        inboundKanbanLabelRepository.saveAll(labels);

        // 10. 更新出库明细 actualQty
        for (Map.Entry<Long, List<InboundKanbanLabel>> entry : detailLabelsMap.entrySet()) {
            OutboundOrderDetail detail = detailMap.get(entry.getKey());
            int totalIssueQty = entry.getValue().stream()
                    .mapToInt(l -> {
                        Integer override = labelIssueQtys != null ? labelIssueQtys.get(l.getId()) : null;
                        return override != null ? override : safeInt(l.getLabelQty());
                    }).sum();
            detail.setActualQty(safeInt(detail.getActualQty()) + totalIssueQty);
            detail.setUpdatedBy(currentOperator);
            detail.setUpdatedAt(now);
        }
        outboundOrderDetailRepository.saveAll(details);

        // 11. 重新计算出库单汇总状态
        List<OutboundOrderDetail> updatedDetails = outboundOrderDetailRepository
                .findByOutboundOrderIdOrderByLineNoAsc(orderId);
        order.setItemCount(updatedDetails.size());
        order.setPlannedTotalQty(updatedDetails.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum());
        order.setActualTotalQty(updatedDetails.stream().mapToInt(item -> safeInt(item.getActualQty())).sum());
        order.setStatus(calculateStatus(order.getPlannedTotalQty(), order.getActualTotalQty()));
        order.setUpdatedBy(currentOperator);
        order.setUpdatedAt(now);
        outboundOrderRepository.save(order);

        return new OutboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(updatedDetails),
                toInventoryStockDTOs(updatedDetails));
    }

    @Override
    public List<OutboundIssuedLabelDTO> getIssuedLabels(Long orderId) {
        OutboundOrder order = findOrder(orderId);
        List<OutboundHistory> histories = outboundHistoryRepository.findByOutboundOrderId(orderId);

        List<OutboundIssuedLabelDTO> result = new ArrayList<>();
        for (OutboundHistory history : histories) {
            // 只显示已出库未退库的
            if (!"已出库".equals(history.getStatus())) {
                continue;
            }
            // 通过 kanbanLabelId 查看看板
            if (history.getKanbanLabelId() == null) {
                continue;
            }
            InboundKanbanLabel label = inboundKanbanLabelRepository
                    .findById(history.getKanbanLabelId()).orElse(null);
            if (label == null) {
                continue;
            }

            OutboundIssuedLabelDTO dto = new OutboundIssuedLabelDTO();
            dto.setLabelId(label.getId());
            dto.setKanbanNo(label.getKanbanNo());
            dto.setMaterialCode(label.getMaterialCode());
            dto.setMaterialName(label.getMaterialName());
            dto.setSupplierName(label.getSupplierName());
            dto.setLabelQty(safeInt(label.getLabelQty()));
            dto.setHistoryId(history.getId());
            dto.setIssueQty(safeInt(history.getIssueQty()));
            dto.setIssuedAt(history.getCreatedAt());
            dto.setSourceInboundDoc(history.getSourceInboundDoc());
            dto.setWarehouseArea(label.getWarehouseArea());
            dto.setOutboundDetailId(history.getOutboundDetailId());
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse returnByLabels(Long orderId, List<Long> labelIds, String operator) {
        if (labelIds == null || labelIds.isEmpty()) {
            throw new IllegalArgumentException("退库看板不能为空");
        }

        OutboundOrder order = findOrder(orderId);

        // 查出库单所有明细行
        List<OutboundOrderDetail> details = outboundOrderDetailRepository
                .findByOutboundOrderIdOrderByLineNoAsc(orderId);

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);

        // 按出库明细分组统计退库数量
        Map<Long, Integer> detailReturnQtyMap = new HashMap<>();
        Map<Long, OutboundOrderDetail> detailMap = new HashMap<>();
        for (OutboundOrderDetail d : details) {
            detailMap.put(d.getId(), d);
        }

        for (Long labelId : labelIds) {
            InboundKanbanLabel label = inboundKanbanLabelRepository.findById(labelId).orElse(null);
            if (label == null) {
                throw new IllegalArgumentException("看板不存在：" + labelId);
            }
            if (!TRANSFER_STATUS_ISSUED.equals(label.getTransferStatus())) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 不是已出库状态，无法退库");
            }

            // 通过 kanbanLabelId 精确查找出库历史
            List<OutboundHistory> labelHistories = outboundHistoryRepository
                    .findByKanbanLabelId(labelId);
            OutboundHistory target = null;
            for (OutboundHistory h : labelHistories) {
                if ("已出库".equals(h.getStatus())) {
                    target = h;
                    break;
                }
            }
            if (target == null) {
                throw new IllegalStateException("看板 " + label.getKanbanNo() + " 未找到对应的出库记录或已退库");
            }

            int returnQty = safeInt(target.getIssueQty());

            // 库存回增
            String returnArea = defaultIfBlank(label.getWarehouseArea(), "默认库区");
            InventoryStock stock = inventoryStockRepository
                    .findByMaterialCodeAndSupplierAndWarehouseArea(
                            label.getMaterialCode(), label.getSupplierName(), returnArea)
                    .orElse(null);
            if (stock == null) {
                stock = new InventoryStock();
                stock.setMaterialCode(label.getMaterialCode());
                stock.setMaterialName(label.getMaterialName());
                stock.setSupplier(label.getSupplierName());
                stock.setOnHandQty(returnQty);
                stock.setLastInboundDocNo(target.getDocNo());
                stock.setLastInboundAt(now);
                stock.setWarehouseArea(label.getWarehouseArea());
                stock.setTransferStatus("不转包");
                stock.setCreatedBy(currentOperator);
                stock.setUpdatedBy(currentOperator);
                stock.setCreatedAt(now);
                stock.setUpdatedAt(now);
            } else {
                stock.setOnHandQty(safeInt(stock.getOnHandQty()) + returnQty);
                stock.setUpdatedBy(currentOperator);
                stock.setUpdatedAt(now);
            }
            inventoryStockRepository.save(stock);

            // 出库历史标记已退库
            target.setStatus(STATUS_RETURNED);
            outboundHistoryRepository.save(target);

            // 看板标签恢复
            label.setLabelStatus(LABEL_STATUS_RECEIVED);
            label.setTransferStatus(null);
            inboundKanbanLabelRepository.save(label);

            // 统计退库数量
            Long detailId = target.getOutboundDetailId();
            detailReturnQtyMap.merge(detailId, returnQty, Integer::sum);
        }

        // 更新出库明细实发数量
        for (Map.Entry<Long, Integer> entry : detailReturnQtyMap.entrySet()) {
            OutboundOrderDetail detail = detailMap.get(entry.getKey());
            if (detail != null) {
                int newActualQty = Math.max(safeInt(detail.getActualQty()) - entry.getValue(), 0);
                detail.setActualQty(newActualQty);
                detail.setUpdatedBy(currentOperator);
                detail.setUpdatedAt(now);
            }
        }
        outboundOrderDetailRepository.saveAll(details);

        // 重新计算出库单汇总状态
        List<OutboundOrderDetail> updatedDetails = outboundOrderDetailRepository
                .findByOutboundOrderIdOrderByLineNoAsc(orderId);
        int plannedTotal = updatedDetails.stream().mapToInt(d -> safeInt(d.getPlannedQty())).sum();
        int actualTotal = updatedDetails.stream().mapToInt(d -> safeInt(d.getActualQty())).sum();

        boolean allReturned = true;
        for (OutboundOrderDetail detail : updatedDetails) {
            List<OutboundHistory> detailHistories = outboundHistoryRepository
                    .findByOutboundDetailId(detail.getId());
            boolean thisLineAllReturned = detailHistories.stream()
                    .allMatch(h -> STATUS_RETURNED.equals(h.getStatus()));
            if (!thisLineAllReturned) {
                allReturned = false;
                break;
            }
        }

        order.setPlannedTotalQty(plannedTotal);
        order.setActualTotalQty(actualTotal);
        if (allReturned) {
            order.setStatus(STATUS_RETURNED);
        } else {
            order.setStatus(calculateStatus(plannedTotal, actualTotal));
        }
        order.setUpdatedBy(currentOperator);
        order.setUpdatedAt(now);
        outboundOrderRepository.save(order);

        return new OutboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(updatedDetails),
                toInventoryStockDTOs(updatedDetails));
    }
}
