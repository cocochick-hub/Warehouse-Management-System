package com.example.wms.dto.demand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 需求明细 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandDetailDTO {

    private Long id;
    private Long batchId;
    private String batchNo;
    private String materialCode;
    private String materialName;
    private String supplierCode;
    private String supplierName;
    private Integer demandQty;
    private Integer fulfilledQty;
    private LocalDate demandDate;
    private String warehouseArea;
    private String status;
    private String remark;
    private LocalDateTime createdAt;

    /** 前端展示用字段 */
    private String statusLabel;
    private String statusType;
}
