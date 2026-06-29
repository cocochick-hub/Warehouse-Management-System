package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.seal.SealLabelDTO;
import com.example.wms.dto.transfer.TransferHistoryPageResponse;
import com.example.wms.dto.transfer.TransferKanbanPageResponse;
import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.dto.transfer.TransferResultDTO;
import com.example.wms.service.SealService;
import com.example.wms.service.TransferService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 转包管理控制器
 *
 * GET  /api/transfer/label       — 扫码查询源看板信息（复用封存接口）
 * GET  /api/transfer/kanbans     — 分页查询可转包的看板列表
 * GET  /api/transfer/history     — 分页查询转包历史记录
 * POST /api/transfer/execute     — 执行转包
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

    /**
     * 分页查询可转包的看板列表
     * 条件：状态为"已入库"且未封存
     *
     * @param materialCode 物料编码（可选，模糊匹配）
     * @param supplierName 供应商名称（可选，模糊匹配）
     * @param page 页码，默认1
     * @param size 每页大小，默认10
     */
    @GetMapping("/kanbans")
    public ApiResult<TransferKanbanPageResponse> listAvailableKanbans(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResult.success(transferService.listAvailableKanbans(materialCode, supplierName, page, size));
    }

    /**
     * 分页查询转包历史记录
     *
     * @param sourceKanbanNo 源看板号（可选，模糊匹配）
     * @param targetKanbanNo 目标看板号（可选，模糊匹配）
     * @param page 页码，默认1
     * @param size 每页大小，默认10
     */
    @GetMapping("/history")
    public ApiResult<TransferHistoryPageResponse> listTransferHistory(
            @RequestParam(required = false) String sourceKanbanNo,
            @RequestParam(required = false) String targetKanbanNo,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResult.success(transferService.listTransferHistory(sourceKanbanNo, targetKanbanNo, page, size));
    }

    /** 执行转包操作 */
    @PostMapping("/execute")
    public ApiResult<TransferResultDTO> executeTransfer(@RequestBody TransferRequest request,
                                                         Authentication auth) {
        String operator = auth != null ? auth.getName() : "system";
        return ApiResult.success(transferService.executeTransfer(request, operator));
    }
}
