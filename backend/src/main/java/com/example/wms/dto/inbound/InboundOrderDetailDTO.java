package com.example.wms.dto.inbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundOrderDetailDTO {

    private Long id;
    private Integer lineNo;
    private String supplierCode;
    private String supplierName;
    private String materialCode;
    private String materialName;
    private String packageModel;
    private Integer packagingCapacity;
    private Integer plannedQty;
    private Integer actualQty;
    private Integer pendingQty;
    private Integer packageCount;
    private Double boxCount;
    private String warehouseArea;
    private String transferStatus;
    private String remark;
}
