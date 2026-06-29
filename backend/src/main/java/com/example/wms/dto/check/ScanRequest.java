package com.example.wms.dto.check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanRequest {

    /** 盘点任务ID */
    private Long taskId;

    /** 物料编码（扫码获取） */
    private String materialCode;

    /** 实盘数量 */
    private Integer actualQty;

    /** 库区 */
    private String warehouseArea;

    /** 盘点人 */
    private String checkedBy;
}