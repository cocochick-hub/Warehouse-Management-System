package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.inbound.InboundOrderCreateRequest;
import com.example.wms.dto.inbound.InboundOrderDetailResponse;
import com.example.wms.dto.inbound.InboundOrderPageResponse;
import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.inbound.InboundReceiveRequest;
import com.example.wms.service.InboundOrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/inbound/orders")
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    public InboundOrderController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @GetMapping
    public ApiResult<InboundOrderPageResponse> listOrders(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        InboundOrderPageResponse data = inboundOrderService.listOrders(docNo, supplier, status, page, size);
        return ApiResult.success(data);
    }

    @GetMapping("/{id}")
    public ApiResult<InboundOrderDetailResponse> getOrderDetail(@PathVariable Long id) {
        return ApiResult.success(inboundOrderService.getOrderDetail(id));
    }

    @PostMapping
    public ApiResult<InboundOrderDetailResponse> createOrder(@Valid @RequestBody InboundOrderCreateRequest request) {
        InboundOrderDetailResponse data = inboundOrderService.createOrder(request, currentUsername());
        return ApiResult.success("入库单创建成功", data);
    }

    @PostMapping("/{id}/receive")
    public ApiResult<InboundOrderDetailResponse> receiveOrder(
            @PathVariable Long id,
            @Valid @RequestBody InboundReceiveRequest request
    ) {
        InboundOrderDetailResponse data = inboundOrderService.receiveOrder(id, request, currentUsername());
        return ApiResult.success("手工入库成功", data);
    }

    @PostMapping("/{id}/kanban-labels/generate")
    public ApiResult<List<InboundKanbanLabelDTO>> generateKanbanLabels(@PathVariable Long id) {
        return ApiResult.success("看板生成成功", inboundOrderService.generateKanbanLabels(id, currentUsername()));
    }

    @GetMapping("/{id}/kanban-labels")
    public ApiResult<List<InboundKanbanLabelDTO>> listKanbanLabels(@PathVariable Long id) {
        return ApiResult.success(inboundOrderService.listKanbanLabels(id));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }
}
