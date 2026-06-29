package com.example.wms.dto.check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckTaskDTO {

    private Long id;
    private String taskNo;
    private String taskName;
    private String checkType;
    private String status;
    private String warehouseArea;
    private String materialCode;
    private String createdBy;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联的明细数 */
    private int detailCount;

    /** 已盘数 */
    private int checkedCount;

    /** 进度百分比 */
    private int progressPercent;
}