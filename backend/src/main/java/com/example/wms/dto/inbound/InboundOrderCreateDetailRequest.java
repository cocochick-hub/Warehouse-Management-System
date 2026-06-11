package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class InboundOrderCreateDetailRequest {

    @NotBlank(message = "供应商代码不能为空")
    @Size(max = 50, message = "供应商代码长度不能超过50")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称长度不能超过100")
    private String supplierName;

    @NotBlank(message = "物料号不能为空")
    @Size(max = 50, message = "物料号长度不能超过50")
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 100, message = "物料名称长度不能超过100")
    private String materialName;

    @Size(max = 50, message = "包装型号长度不能超过50")
    private String packageModel;

    @Min(value = 0, message = "包装容量不能小于0")
    private Integer packagingCapacity;

    @NotNull(message = "计划入库数量不能为空")
    @Min(value = 1, message = "计划入库数量必须大于0")
    private Integer plannedQty;

    @Min(value = 1, message = "包数必须大于0")
    private Integer packageCount;

    @Size(max = 100, message = "库区长度不能超过100")
    private String warehouseArea;

    @Size(max = 20, message = "转包状态长度不能超过20")
    private String transferStatus;

    @Size(max = 255, message = "明细备注长度不能超过255")
    private String remark;
}
