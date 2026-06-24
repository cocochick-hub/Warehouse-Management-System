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
        int consumedQty = consumed.stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();
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
                availableQty
        );
    }

    @Override
    @Transactional
    public OutboundOrderDetailResponse issueByScan(OutboundScanIssueRequest request, String operator) {
        InboundKanbanLabel label = findKanbanLabel(request.getKanbanNo());
        if (!LABEL_STATUS_RECEIVED.equals(label.getLabelStatus())) {
            throw new IllegalStateException("该看板尚未完成入库，无法出库");
        }

        if (request.getOutboundOrderId() == null) {
            throw new IllegalArgumentException("出库单ID不能为空");
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
        int consumedQty = consumed.stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();
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
        history.setWarehouseArea(warehouseArea);
        history.setIssuedBy(currentOperator);
        history.setCreatedAt(now);
        outboundHistoryRepository.save(history);

        matchingDetail.setActualQty(nextActualQty);
        matchingDetail.setUpdatedBy(currentOperator);
        matchingDetail.setUpdatedAt(now);
        outboundOrderDetailRepository.save(matchingDetail);

        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplier(matchingDetail.getMaterialCode(), matchingDetail.getSupplierName())
                .orElse(null);
        if (stock == null) {
            throw new IllegalStateException(
                    "物料 " + matchingDetail.getMaterialCode() + "（需求方 " + matchingDetail.getSupplierName() + "）不存在库存记录，请先入库");
        }
        int onHand = safeInt(stock.getOnHandQty());
        if (onHand < issueQty) {
            throw new IllegalStateException(
                    "物料 " + matchingDetail.getMaterialCode() + " 当前库存 " + onHand + "，不足 " + issueQty);
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
            int consumedQty = consumed.stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();
            int availableQty = safeInt(id.getActualQty()) - consumedQty;
            totalAvailable += Math.max(availableQty, 0);
        }

        if (totalAvailable < issueQty) {
            throw new IllegalStateException(
                    "物料 " + detail.getMaterialCode() + " 库存不足，需要 " + issueQty
                            + "，可用 " + totalAvailable);
        }

        List<OutboundHistory> records = new ArrayList<>();
        int remaining = issueQty;
        for (InboundOrderDetail id : eligible) {
            if (remaining <= 0) {
                break;
            }

            List<OutboundHistory> consumed = outboundHistoryRepository.findBySourceDetailId(id.getId());
            int consumedQty = consumed.stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();
            int availableQty = safeInt(id.getActualQty()) - consumedQty;
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
            history.setIssuedBy(operator);
            history.setCreatedAt(now);
            records.add(history);

            remaining -= allocateQty;
        }

        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplier(detail.getMaterialCode(), detail.getSupplierName())
                .orElse(null);
        if (stock == null) {
            throw new IllegalStateException(
                    "物料 " + detail.getMaterialCode() + "（需求方 " + detail.getSupplierName() + "）不存在库存记录，请先入库");
        }
        int onHand = safeInt(stock.getOnHandQty());
        if (onHand < issueQty) {
            throw new IllegalStateException(
                    "物料 " + detail.getMaterialCode() + " 当前库存 " + onHand + "，不足 " + issueQty);
        }
        stock.setOnHandQty(onHand - issueQty);
        stock.setUpdatedBy(operator);
        stock.setUpdatedAt(now);
        inventoryStockRepository.save(stock);

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

        List<InventoryStock> stocks = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (OutboundOrderDetail detail : details) {
            String key = detail.getSupplierName() + "::" + detail.getMaterialCode();
            if (!seenKeys.add(key)) {
                continue;
            }
            inventoryStockRepository.findByMaterialCodeAndSupplier(detail.getMaterialCode(), detail.getSupplierName())
                    .ifPresent(stocks::add);
        }

        return stocks.stream()
                .map(stock -> new InventoryStockDTO(
                        stock.getMaterialCode(),
                        stock.getMaterialName(),
                        stock.getSupplier(),
                        stock.getOnHandQty(),
                        stock.getLastInboundDocNo(),
                        stock.getLastInboundAt(),
                        null,
                        null
                ))
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
                history.getCreatedAt()
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

    @Override
    public OutboundReturnLabelResponse getReturnLabel(String kanbanNo) {
        InboundKanbanLabel label = findKanbanLabel(kanbanNo);

        // 查找该看板对应的入库明细
        InboundOrderDetail inboundDetail = inboundOrderDetailRepository
                .findById(label.getInboundOrderDetailId())
                .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));

        // 查找该入库明细对应的所有出库历史（按 sourceDetailId 关联）
        List<OutboundHistory> histories = outboundHistoryRepository
                .findBySourceDetailId(inboundDetail.getId());

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

        InboundOrderDetail inboundDetail = inboundOrderDetailRepository
                .findById(label.getInboundOrderDetailId())
                .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));

        List<OutboundHistory> histories = outboundHistoryRepository
                .findBySourceDetailId(inboundDetail.getId());

        if (histories.isEmpty()) {
            throw new IllegalStateException("该看板对应的物料未被出库过");
        }

        OutboundHistory latestHistory = histories.stream()
                .filter(h -> h.getCreatedAt() != null)
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(histories.get(0));

        if (STATUS_RETURNED.equals(latestHistory.getStatus())) {
            throw new IllegalStateException("该看板已退库，无法重复退库");
        }

        int returnQty = safeInt(latestHistory.getIssueQty());

        // 1. 库存回增
        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplier(label.getMaterialCode(), label.getSupplierName())
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
        } else {
            stock.setOnHandQty(safeInt(stock.getOnHandQty()) + returnQty);
            stock.setUpdatedBy(normalizeOperator(operator));
            stock.setUpdatedAt(LocalDateTime.now());
        }
        inventoryStockRepository.save(stock);

        // 2. 出库历史标记已退库
        latestHistory.setStatus(STATUS_RETURNED);
        outboundHistoryRepository.save(latestHistory);

        // 3. 检查出库单是否全部退库
        OutboundOrder order = outboundOrderRepository.findById(latestHistory.getOutboundOrderId()).orElse(null);
        if (order != null) {
            List<OutboundOrderDetail> orderDetails = outboundOrderDetailRepository
                    .findByOutboundOrderIdOrderByLineNoAsc(order.getId());
            boolean allReturned = true;
            for (OutboundOrderDetail detail : orderDetails) {
                List<OutboundHistory> detailHistories = outboundHistoryRepository
                        .findBySourceDetailId(detail.getId());
                boolean thisLineAllReturned = detailHistories.stream()
                        .allMatch(h -> STATUS_RETURNED.equals(h.getStatus()));
                if (!thisLineAllReturned) {
                    allReturned = false;
                    break;
                }
            }
            if (allReturned) {
                order.setStatus(STATUS_RETURNED);
                outboundOrderRepository.save(order);
            }
        }

        return new OutboundReturnResponse(
                latestHistory.getDocNo(),
                label.getMaterialCode(),
                returnQty,
                safeInt(stock.getOnHandQty()));
    }
}
