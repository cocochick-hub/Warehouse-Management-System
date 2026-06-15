package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundOrderSummaryDTO {

    private Long id;
    private String docNo;
    private String supplier;
    private String status;
    private int itemCount;
    private int plannedTotalQty;
    private int actualTotalQty;
    private String remark;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
