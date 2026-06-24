package com.example.wms.dto.outbound;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class OutboundOrderCreateRequest {

    @Size(max = 100, message = "需求方长度不能超过100")
    private String supplier;

    private String outboundType;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @Valid
    private List<OutboundCreateDetailRequest> details;
}
