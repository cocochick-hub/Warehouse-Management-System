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
    private static final DateTimeFormatter DOC_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger DOC_NO_SEQUENCE = new AtomicInteger(0);

    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderDetailRepository outboundOrderDetailRepository;
    private final OutboundHistoryRepository outboundHistoryRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderDetailRepository inboundOrderDetailRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public OutboundOrderServiceImpl(OutboundOrderRepository outboundOrderRepository,
                                    OutboundOrderDetailRepository outboundOrderDetailRepository,
                                    OutboundHistoryRepository outboundHistoryRepository,
                                    InboundOrderRepository inboundOrderRepository,
                                    InboundOrderDetailRepository inboundOrderDetailRepository,
                                    InventoryStockRepository inventoryStockRepository) {
        this.outboundOrderRepository = outboundOrderRepository;
        this.outboundOrderDetailRepository = outboundOrderDetailRepository;
        this.outboundHistoryRepository = outboundHistoryRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.inboundOrderDetailRepository = inboundOrderDetailRepository;
        this.inventoryStockRepository = inventoryStockRepository;
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

    // ==================== Private Helpers ====================

    private OutboundOrder findOrder(Long id) {
        return outboundOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("出库单不存在"));
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
}
