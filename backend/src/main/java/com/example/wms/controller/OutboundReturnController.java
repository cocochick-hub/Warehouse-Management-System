package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.outbound.OutboundReturnLabelResponse;
import com.example.wms.dto.outbound.OutboundReturnRequest;
import com.example.wms.dto.outbound.OutboundReturnResponse;
import com.example.wms.service.OutboundOrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/outbound/return")
public class OutboundReturnController {

    private final OutboundOrderService outboundOrderService;

    public OutboundReturnController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @GetMapping("/labels/{kanbanNo}")
    public ApiResult<OutboundReturnLabelResponse> getReturnLabel(@PathVariable String kanbanNo) {
        return ApiResult.success(outboundOrderService.getReturnLabel(kanbanNo));
    }

    @PostMapping
    public ApiResult<OutboundReturnResponse> returnByScan(@Valid @RequestBody OutboundReturnRequest request) {
        OutboundReturnResponse data = outboundOrderService.returnByScan(request, currentUsername());
        return ApiResult.success("退库成功", data);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }
}
