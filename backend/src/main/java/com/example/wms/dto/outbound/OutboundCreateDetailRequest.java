package com.example.wms.dto.outbound;

import lombok.Data;

@Data
public class OutboundCreateDetailRequest {

    private String supplierCode;
    private String supplierName;
    private String materialCode;
    private String materialName;
    private int plannedQty;
    private String warehouseArea;
    private String remark;
}
