package com.example.wms.dto.alert;

import lombok.Data;

import java.util.List;

/** 批量保存阈值请求 */
@Data
public class ThresholdBatchRequest {
    private List<AlertThresholdDTO> items;
    private String operator;
}
