package com.example.wms.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 转包历史记录DTO
 * 用于展示转包操作的历史记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferHistoryDTO {

    /** 转包记录ID */
    private Long id;

    /** 源看板号 */
    private String sourceKanbanNo;

    /** 目标看板号 */
    private String targetKanbanNo;

    /** 转移数量 */
    private Integer transferQty;

    /** 转移前源看板可用数量 */
    private Integer sourceQtyBefore;

    /** 转移后源看板可用数量 */
    private Integer sourceQtyAfter;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 供应商名称 */
    private String supplierName;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime createdAt;

    /** 源出库单号 */
    private String sourceOutboundDocNo;

    /** 目标入库单号 */
    private String targetInboundDocNo;

    /** 转包类型：拆包 / 合包 */
    private String transferType;
}
