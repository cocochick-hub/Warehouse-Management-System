package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.outbound.OutboundOrderDetailResponse;
import com.example.wms.dto.outbound.OutboundOrderlessRequest;
import com.example.wms.dto.outbound.OutboundScanIssueRequest;
import com.example.wms.dto.outbound.OutboundScanLabelResponse;
import com.example.wms.service.OutboundOrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/outbound/scan")
public class OutboundScanController {

    private final OutboundOrderService outboundOrderService;

    public OutboundScanController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @GetMapping("/labels/{kanbanNo}")
    public ApiResult<OutboundScanLabelResponse> getScanLabel(@PathVariable String kanbanNo) {
        return ApiResult.success(outboundOrderService.getOutboundScanLabel(kanbanNo));
    }

    @PostMapping("/issue")
    public ApiResult<OutboundOrderDetailResponse> issueByScan(@Valid @RequestBody OutboundScanIssueRequest request) {
        OutboundOrderDetailResponse data = outboundOrderService.issueByScan(request, currentUsername());
        return ApiResult.success("扫码出库成功", data);
    }

    @PostMapping("/orderless-issue")
    public ApiResult<OutboundOrderDetailResponse> issueWithoutOrder(@Valid @RequestBody OutboundOrderlessRequest request) {
        OutboundOrderDetailResponse data = outboundOrderService.issueWithoutOrder(request, currentUsername());
        return ApiResult.success("不带单出库成功", data);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }
}
