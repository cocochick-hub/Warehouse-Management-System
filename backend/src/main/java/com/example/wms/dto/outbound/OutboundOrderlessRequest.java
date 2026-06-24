package com.example.wms.dto.outbound;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OutboundOrderlessRequest {

    @NotBlank(message = "物料代码不能为空")
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    private String materialName;

    @NotBlank(message = "需求方代码不能为空")
    private String supplierCode;

    @NotBlank(message = "需求方名称不能为空")
    private String supplierName;

    @NotNull(message = "出库数量不能为空")
    private Integer issueQty;

    private String warehouseArea;

    private String remark;
}
