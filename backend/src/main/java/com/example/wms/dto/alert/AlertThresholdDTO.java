package com.example.wms.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 高低储预警阈值 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertThresholdDTO {

    private Long id;
    private String materialCode;
    private String materialName;
    private String supplier;
    private Integer lowStockQty;
    private Integer highStockQty;
    private Integer currentStock;      // 当前库存（仅查询时返回）
    private String alertStatus;         // low / high / normal（仅查询时返回）
    private LocalDateTime updatedAt;
}
