package com.example.wms.dto.seal;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SealLabelDTO {

    private Long id;
    private String kanbanNo;
    private String docNo;
    private String materialCode;
    private String materialName;
    private String supplierName;
    private Integer labelQty;
    private String warehouseArea;
    private String labelStatus;
    private String transferStatus;
    private Boolean sealed;
    private LocalDateTime sealedAt;
    private String sealedBy;
    private Integer availableQty;
}
