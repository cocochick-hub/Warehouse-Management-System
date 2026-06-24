package com.example.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private int pendingInbound;
    private int pendingOutbound;
    private int lowStockAlert;
    private int totalMaterials;
    private List<PendingTaskDTO> pendingTasks;

    // 库存健康度（从 alert_threshold 计算）
    private int healthPercent;
    private int normalCount;
    private int lowAlertCount;
    private int highAlertCount;
}
