package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundOrderDetailDTO {

    private Long id;
    private int lineNo;
    private String supplierCode;
    private String supplierName;
    private String materialCode;
    private String materialName;
    private int plannedQty;
    private int actualQty;
    private int pendingQty;
    private String warehouseArea;
    private String remark;
}
