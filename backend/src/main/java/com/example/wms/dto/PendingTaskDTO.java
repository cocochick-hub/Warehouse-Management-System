package com.example.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingTaskDTO {

    private String type;
    private String docNo;
    private String supplier;
    private String status;
    private String statusColor;
    private String date;
}
