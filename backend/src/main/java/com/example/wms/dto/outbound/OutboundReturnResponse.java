package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退库成功响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundReturnResponse {

    /** 原出库单号 */
    private String outboundDocNo;

    /** 物料号 */
    private String materialCode;

    /** 退库数量 */
    private Integer returnQty;

    /** 退库后当前库存 */
    private Integer currentStock;
}
