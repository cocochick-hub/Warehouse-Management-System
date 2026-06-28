package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.seal.SealLabelDTO;
import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.dto.transfer.TransferResultDTO;
import com.example.wms.service.SealService;
import com.example.wms.service.TransferService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 转包管理控制器
 *
 * GET  /api/transfer/label?kanbanNo=xxx  — 扫码查询源看板信息（复用封存接口）
 * POST /api/transfer/execute              — 执行转包
 */
@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    private final TransferService transferService;
    private final SealService sealService;

    public TransferController(TransferService transferService, SealService sealService) {
        this.transferService = transferService;
        this.sealService = sealService;
    }

    /** 扫码查询看板信息（转包入口，复用封存的查询逻辑） */
    @GetMapping("/label")
    public ApiResult<SealLabelDTO> getLabel(@RequestParam String kanbanNo) {
        return ApiResult.success(sealService.getLabelByKanbanNo(kanbanNo));
    }

    /** 执行转包操作 */
    @PostMapping("/execute")
    public ApiResult<TransferResultDTO> executeTransfer(@RequestBody TransferRequest request,
                                                         Authentication auth) {
        String operator = auth != null ? auth.getName() : "system";
        return ApiResult.success(transferService.executeTransfer(request, operator));
    }
}
