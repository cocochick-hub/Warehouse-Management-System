package com.example.wms.dto.demand;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 手工创建需求请求
 */
@Data
public class DemandCreateRequest {

    /** 明细列表 */
    @NotNull(message = "需求明细不能为空")
    private List<DemandItem> items;

    @Data
    public static class DemandItem {
        @NotBlank(message = "物料号不能为空")
        private String materialCode;

        @NotBlank(message = "物料名称不能为空")
        private String materialName;

        @NotBlank(message = "供应商代码不能为空")
        private String supplierCode;

        @NotBlank(message = "供应商名称不能为空")
        private String supplierName;

        @NotNull(message = "需求数量不能为空")
        private Integer demandQty;

        private LocalDate demandDate;

        private String warehouseArea;

        private String remark;
    }
}
