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
}
