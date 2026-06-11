package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.inbound.InboundOrderDetailResponse;
import com.example.wms.dto.inbound.InboundScanReceiveRequest;
import com.example.wms.service.InboundOrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/inbound/scan")
public class InboundScanController {

    private final InboundOrderService inboundOrderService;

    public InboundScanController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @GetMapping("/labels/{kanbanNo}")
    public ApiResult<InboundKanbanLabelDTO> getScanLabel(@PathVariable String kanbanNo) {
        return ApiResult.success(inboundOrderService.getScanLabel(kanbanNo));
    }

    @PostMapping("/receive")
    public ApiResult<InboundOrderDetailResponse> receiveByScan(@Valid @RequestBody InboundScanReceiveRequest request) {
        InboundOrderDetailResponse data = inboundOrderService.receiveByScan(request, currentUsername());
        return ApiResult.success("扫码入库成功", data);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }
}
