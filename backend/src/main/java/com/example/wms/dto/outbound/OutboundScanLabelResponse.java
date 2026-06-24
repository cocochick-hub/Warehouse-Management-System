package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundScanLabelResponse {

    private Long id;
    private String kanbanNo;
    private String docNo;
    private String materialCode;
    private String materialName;
    private String supplierCode;
    private String supplierName;
    private Integer labelQty;
    private String warehouseArea;
    private String labelStatus;
    private Long inboundOrderId;
    private Long inboundDetailId;
    private Boolean fifoWarning;
    private String fifoMessage;
    private String earliestDocNo;
    private Integer availableQty;
    private Boolean sealed;
    private String sealedMessage;
}
