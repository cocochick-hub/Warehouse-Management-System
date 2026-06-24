package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.seal.SealBatchResultDTO;
import com.example.wms.dto.seal.SealLabelDTO;
import com.example.wms.dto.seal.SealLabelRequest;
import com.example.wms.service.SealService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seal")
public class SealController {

    private final SealService sealService;

    public SealController(SealService sealService) {
        this.sealService = sealService;
    }

    /** 根据看板号查询封存信息（扫码时调用） */
    @GetMapping("/label")
    public ApiResult<SealLabelDTO> getLabel(@RequestParam String kanbanNo) {
        return ApiResult.success(sealService.getLabelByKanbanNo(kanbanNo));
    }

    /** 查询已封存的看板列表 */
    @GetMapping("/sealed-labels")
    public ApiResult<List<SealLabelDTO>> listSealedLabels(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String supplierName) {
        return ApiResult.success(sealService.listSealedLabels(materialCode, supplierName));
    }

    /** 单个封存/解封（扫码操作） */
    @PostMapping("/toggle")
    public ApiResult<SealLabelDTO> toggleSeal(@RequestBody SealLabelRequest request,
                                               Authentication auth) {
        String operator = auth != null ? auth.getName() : "system";
        return ApiResult.success(sealService.toggleSealSingle(request.getKanbanNo(), request.getAction(), operator));
    }

    /** 批量封存/解封（输入看板号列表） */
    @PostMapping("/toggle-batch")
    public ApiResult<SealBatchResultDTO> toggleSealBatch(@RequestBody SealLabelRequest request,
                                                          Authentication auth) {
        String operator = auth != null ? auth.getName() : "system";
        return ApiResult.success(sealService.toggleSealBatch(request, operator));
    }
}
