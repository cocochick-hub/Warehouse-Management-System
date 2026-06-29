package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundKanbanLabelDTO {

    // 看板信息
    private Long id;
    private String kanbanNo;
    private String docNo;              // 来源入库单号
    private String materialCode;
    private String materialName;
    private String supplierCode;
    private String supplierName;
    private Integer labelQty;          // 本包数量
    private Integer availableQty;      // 可用数量
    private Integer packageSeq;
    private Integer packageTotal;
    private String warehouseArea;
    private String labelStatus;
    private String transferStatus;
    private Long inboundOrderId;
    private Long inboundOrderDetailId;
    private Boolean sealed;
    private LocalDateTime createdAt;

    // 匹配的出库明细信息
    private Long matchedOutboundDetailId;
    private String matchedDetailInfo;  // "M001 plannedQty=100 actualQty=0 pendingQty=100"
}
