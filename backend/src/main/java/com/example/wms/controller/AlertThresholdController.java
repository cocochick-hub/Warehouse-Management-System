package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.alert.AlertThresholdDTO;
import com.example.wms.dto.alert.ThresholdBatchRequest;
import com.example.wms.service.AlertThresholdService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 高低储预警阈值管理 Controller
 */
@RestController
@RequestMapping("/api/alert")
public class AlertThresholdController {

    private final AlertThresholdService alertThresholdService;

    public AlertThresholdController(AlertThresholdService alertThresholdService) {
        this.alertThresholdService = alertThresholdService;
    }

    /**
     * 获取所有物料的阈值列表（含库存状态）
     */
    @GetMapping("/thresholds")
    public ApiResult<List<AlertThresholdDTO>> listThresholds() {
        return ApiResult.success(alertThresholdService.listAll());
    }

    /**
     * 批量保存/更新阈值（admin 编辑）
     */
    @PostMapping("/thresholds")
    public ApiResult<Void> saveThresholds(@RequestBody ThresholdBatchRequest request) {
        String operator = request.getOperator() != null ? request.getOperator() : "system";
        alertThresholdService.batchSave(request.getItems(), operator);
        return ApiResult.success("保存成功", null);
    }
}
