package com.example.wms.service.impl;

import com.example.wms.dto.inbound.*;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.entity.InventoryStock;
import com.example.wms.repository.InboundOrderDetailRepository;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.service.InboundOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InboundOrderServiceImpl implements InboundOrderService {

    private static final String STATUS_PENDING = "未入库";
    private static final String STATUS_PARTIAL = "部分完成";
    private static final String STATUS_COMPLETED = "已完成";

    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderDetailRepository inboundOrderDetailRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public InboundOrderServiceImpl(InboundOrderRepository inboundOrderRepository,
                                   InboundOrderDetailRepository inboundOrderDetailRepository,
                                   InventoryStockRepository inventoryStockRepository) {
        this.inboundOrderRepository = inboundOrderRepository;
        this.inboundOrderDetailRepository = inboundOrderDetailRepository;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    @Override
    public InboundOrderPageResponse listOrders(String docNo, String supplier, String status, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;

        List<InboundOrderSummaryDTO> filtered = inboundOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> matchesText(order.getDocNo(), docNo))
                .filter(order -> matchesText(order.getSupplier(), supplier))
                .filter(order -> matchesStatus(order.getStatus(), status))
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());

        int fromIndex = Math.min((safePage - 1) * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        List<InboundOrderSummaryDTO> records = filtered.subList(fromIndex, toIndex);

        return new InboundOrderPageResponse(filtered.size(), safePage, safeSize, records);
    }

    @Override
    public InboundOrderDetailResponse getOrderDetail(Long id) {
        InboundOrder order = findOrder(id);
        List<InboundOrderDetail> details = inboundOrderDetailRepository.findByInboundOrderIdOrderByLineNoAsc(id);
        return new InboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details));
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
        order.setSupplier(request.getSupplier().trim());
        order.setStatus(STATUS_PENDING);
        order.setItemCount(request.getDetails().size());
        order.setPlannedTotalQty(sumPlannedQty(request.getDetails()));
        order.setActualTotalQty(0);
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
            detail.setMaterialCode(item.getMaterialCode().trim());
            detail.setMaterialName(item.getMaterialName().trim());
            detail.setPackagingCapacity(item.getPackagingCapacity());
            detail.setPlannedQty(item.getPlannedQty());
            detail.setActualQty(0);
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

        order.setItemCount(details.size());
        order.setPlannedTotalQty(details.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum());
        order.setActualTotalQty(details.stream().mapToInt(item -> safeInt(item.getActualQty())).sum());
        order.setStatus(calculateStatus(order.getPlannedTotalQty(), order.getActualTotalQty()));
        order.setUpdatedBy(currentOperator);
        order.setUpdatedAt(now);
        inboundOrderRepository.save(order);

        return new InboundOrderDetailResponse(toSummaryDTO(order), toDetailDTOs(details));
    }

    private InboundOrder findOrder(Long id) {
        return inboundOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("入库单不存在"));
    }

    private void validateCreateRequest(InboundOrderCreateRequest request) {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("入库单至少包含一条明细");
        }

        Set<String> materialCodes = new HashSet<>();
        for (InboundOrderCreateDetailRequest detail : request.getDetails()) {
            String materialCode = trimToNull(detail.getMaterialCode());
            if (materialCode == null) {
                throw new IllegalArgumentException("物料号不能为空");
            }
            if (!materialCodes.add(materialCode)) {
                throw new IllegalArgumentException("同一张入库单中不允许重复选择同一物料");
            }
        }
    }

    private String generateDocNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String docNo = "IN" + LocalDateTime.now().format(formatter);
        while (inboundOrderRepository.existsByDocNo(docNo)) {
            docNo = "IN" + LocalDateTime.now().plusNanos(1_000_000).format(formatter);
        }
        return docNo;
    }

    private int sumPlannedQty(List<InboundOrderCreateDetailRequest> details) {
        return details.stream().mapToInt(item -> safeInt(item.getPlannedQty())).sum();
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

            InventoryStock stock = inventoryStockRepository
                    .findByMaterialCodeAndSupplier(detail.getMaterialCode(), order.getSupplier())
                    .orElseGet(() -> createInventoryStock(order, detail, operator, now));

            stock.setMaterialName(detail.getMaterialName());
            stock.setOnHandQty(safeInt(stock.getOnHandQty()) + receiveQty);
            stock.setLastInboundDocNo(order.getDocNo());
            stock.setLastInboundAt(now);
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
        stock.setMaterialName(detail.getMaterialName());
        stock.setSupplier(order.getSupplier());
        stock.setOnHandQty(0);
        stock.setLastInboundDocNo(null);
        stock.setLastInboundAt(null);
        stock.setCreatedBy(operator);
        stock.setUpdatedBy(operator);
        stock.setCreatedAt(now);
        stock.setUpdatedAt(now);
        return stock;
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
                        detail.getMaterialCode(),
                        detail.getMaterialName(),
                        detail.getPackagingCapacity(),
                        detail.getPlannedQty(),
                        detail.getActualQty(),
                        Math.max(safeInt(detail.getPlannedQty()) - safeInt(detail.getActualQty()), 0),
                        detail.getRemark()
                ))
                .collect(Collectors.toList());
    }

    private boolean matchesText(String source, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        return source != null && source.contains(keyword.trim());
    }

    private boolean matchesStatus(String source, String status) {
        if (status == null || status.trim().isEmpty()) {
            return true;
        }
        return Objects.equals(source, status.trim());
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

    private String normalizeOperator(String operator) {
        return trimToNull(operator) == null ? "system" : operator.trim();
    }
}
