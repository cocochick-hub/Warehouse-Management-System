package com.example.wms.dto.inbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundOrderSummaryDTO {

    private Long id;
    private String docNo;
    private String supplier;
    private String status;
    private Integer itemCount;
    private Integer plannedTotalQty;
    private Integer actualTotalQty;
    private String transferStatus;
    private String remark;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
