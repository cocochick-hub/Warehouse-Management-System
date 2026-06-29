package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundIssuedLabelDTO {

    // 看板信息
    private Long labelId;
    private String kanbanNo;
    private String materialCode;
    private String materialName;
    private String supplierName;
    private Integer labelQty;          // 看板数量

    // 出库历史信息
    private Long historyId;
    private Integer issueQty;          // 出库数量
    private LocalDateTime issuedAt;    // 出库时间

    // 来源信息
    private String sourceInboundDoc;   // 来源入库单号
    private String warehouseArea;      // 库区
    private Long outboundDetailId;     // 匹配的出库明细ID
}
