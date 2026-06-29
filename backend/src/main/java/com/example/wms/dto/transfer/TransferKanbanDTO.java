package com.example.wms.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 转包看板列表项DTO
 * 用于展示可转包的看板信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferKanbanDTO {

    /** 看板标签ID */
    private Long id;

    /** 看板号 */
    private String kanbanNo;

    /** 入库单号 */
    private String docNo;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 包装型号 */
    private String packageModel;

    /** 库区 */
    private String warehouseArea;

    /** 看板数量 */
    private Integer labelQty;

    /** 已消耗数量 */
    private Integer consumedQty;

    /** 可用数量 = labelQty - consumedQty */
    private Integer availableQty;

    /** 标签状态 */
    private String labelStatus;

    /** 是否封存 */
    private Boolean sealed;

    /** 转包状态 */
    private String transferStatus;

    /** 入库时间 */
    private LocalDateTime createdAt;
}
