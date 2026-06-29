package com.example.wms.dto.check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckProgressDTO {

    /** 任务ID */
    private Long taskId;

    /** 盘点单号 */
    private String taskNo;

    /** 盘点名称 */
    private String taskName;

    /** 盘点类型 */
    private String checkType;

    /** 总物料数 */
    private int total;

    /** 已盘物料数 */
    private int checked;

    /** 进度百分比 */
    private int progressPercent;

    /** 差异数（已盘且diff_qty != 0的物料数） */
    private int diffCount;
}