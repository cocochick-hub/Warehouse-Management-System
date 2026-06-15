package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.outbound.OutboundOrderCreateRequest;
import com.example.wms.dto.outbound.OutboundOrderDetailResponse;
import com.example.wms.dto.outbound.OutboundOrderPageResponse;
import com.example.wms.dto.outbound.OutboundHistoryDTO;
import com.example.wms.dto.outbound.OutboundIssueRequest;
import com.example.wms.service.OutboundOrderService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/outbound")
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    public OutboundOrderController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @GetMapping("/orders")
    public ApiResult<OutboundOrderPageResponse> listOrders(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        OutboundOrderPageResponse data = outboundOrderService.listOrders(docNo, supplier, status, page, size);
        return ApiResult.success(data);
    }

    @GetMapping("/orders/{id}")
    public ApiResult<OutboundOrderDetailResponse> getOrderDetail(@PathVariable Long id) {
        return ApiResult.success(outboundOrderService.getOrderDetail(id));
    }

    @PostMapping("/orders")
    public ApiResult<OutboundOrderDetailResponse> createOrder(@Valid @RequestBody OutboundOrderCreateRequest request) {
        OutboundOrderDetailResponse data = outboundOrderService.createOrder(request, currentUsername());
        return ApiResult.success("出库单创建成功", data);
    }

    @PostMapping("/orders/{id}/issue")
    public ApiResult<OutboundOrderDetailResponse> issueOrder(
            @PathVariable Long id,
            @Valid @RequestBody OutboundIssueRequest request
    ) {
        OutboundOrderDetailResponse data = outboundOrderService.issueOrder(id, request, currentUsername());
        return ApiResult.success("出库成功", data);
    }

    @GetMapping("/history")
    public ApiResult<Page<OutboundHistoryDTO>> listHistory(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResult.success(outboundOrderService.listHistory(docNo, page, size));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }
}
