package com.example.wms.dto.check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    /** 盘点名称 */
    private String taskName;

    /** 盘点类型：明盘/盲盘 */
    private String checkType = "明盘";

    /** 盘点库区（空=全库） */
    private String warehouseArea;

    /** 盘点物料（空=全部物料） */
    private String materialCode;
}