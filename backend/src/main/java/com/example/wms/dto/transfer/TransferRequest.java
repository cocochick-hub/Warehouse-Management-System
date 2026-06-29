package com.example.wms.dto.transfer;

import lombok.Data;

/**
 * 转包操作请求
 */
@Data
public class TransferRequest {

    /** 源看板号 */
    private String sourceKanbanNo;

    /** 目标看板号（不填则自动生成 T- 前缀看板号） */
    private String targetKanbanNo;

    /** 转移数量 */
    private Integer transferQty;
}
