package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class InboundOrderCreateDetailRequest {

    @NotBlank(message = "物料号不能为空")
    @Size(max = 50, message = "物料号长度不能超过50")
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 100, message = "物料名称长度不能超过100")
    private String materialName;

    @Min(value = 0, message = "包装容量不能小于0")
    private Integer packagingCapacity;

    @NotNull(message = "计划入库数量不能为空")
    @Min(value = 1, message = "计划入库数量必须大于0")
    private Integer plannedQty;

    @Size(max = 255, message = "明细备注长度不能超过255")
    private String remark;
}
