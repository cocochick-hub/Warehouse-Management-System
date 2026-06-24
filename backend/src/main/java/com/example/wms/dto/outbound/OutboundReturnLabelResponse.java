package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退库查询 — 扫码看板后返回的退库预检信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundReturnLabelResponse {

    /** 看板号 */
    private String kanbanNo;

    /** 物料号 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 供应商名称 */
    private String supplierName;

    /** 原出库单号 */
    private String outboundDocNo;

    /** 原出库数量 */
    private Integer issueQty;

    /** 原出库时间 */
    private String issuedAt;

    /** 库区 */
    private String warehouseArea;

    /** 是否可退库 */
    private Boolean canReturn;

    /** 不可退原因（canReturn=false 时填充） */
    private String reason;
}
