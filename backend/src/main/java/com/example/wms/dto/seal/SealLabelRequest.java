package com.example.wms.dto.seal;

import lombok.Data;

import java.util.List;

@Data
public class SealLabelRequest {

    /** 看板号（扫码单个操作时使用） */
    private String kanbanNo;

    /** 看板号列表（批量操作时使用） */
    private List<String> kanbanNos;

    /** 操作类型：seal-封存 unseal-解封 */
    private String action;
}
