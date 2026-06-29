package com.example.wms.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 转包操作结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferResultDTO {

    /** 源看板号 */
    private String sourceKanbanNo;

    /** 源看板转移后剩余可用数量 */
    private Integer sourceRemainingQty;

    /** 目标看板号 */
    private String targetKanbanNo;

    /** 目标看板数量（拆包=转移数量，合包=累加后总量） */
    private Integer targetQty;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 供应商 */
    private String supplierName;

    /** 库区 */
    private String warehouseArea;

    /** 操作时间 */
    private LocalDateTime transferTime;

    /** 转包类型：拆包 / 合包 */
    private String transferType;
}
