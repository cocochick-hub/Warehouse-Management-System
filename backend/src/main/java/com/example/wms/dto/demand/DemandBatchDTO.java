package com.example.wms.dto.demand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 需求批次 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandBatchDTO {

    private Long id;
    private String batchNo;
    private Integer itemCount;
    private Integer totalQty;
    private String importType;
    private String createdBy;
    private LocalDateTime createdAt;
}
