package com.example.wms.dto.inbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundKanbanLabelDTO {

    private Long id;
    private Long inboundOrderId;
    private Long inboundOrderDetailId;
    private String docNo;
    private String kanbanNo;
    private String qrPayload;
    private String materialCode;
    private String materialName;
    private String supplierCode;
    private String supplierName;
    private String packageModel;
    private String warehouseArea;
    private Integer labelQty;
    private Integer packageSeq;
    private Integer packageTotal;
    private String transferStatus;
    private String labelStatus;
    private LocalDateTime printedAt;
    private LocalDateTime receivedAt;
    private String receivedBy;
    private Boolean sealed;
    private LocalDateTime sealedAt;
    private String sealedBy;
    private Integer availableQty;
}
