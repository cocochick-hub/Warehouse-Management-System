package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class InboundOrderCreateRequest {

    @NotBlank(message = "供应商不能为空")
    @Size(max = 100, message = "供应商长度不能超过100")
    private String supplier;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @Valid
    @NotEmpty(message = "入库单明细不能为空")
    private List<InboundOrderCreateDetailRequest> details;
}
