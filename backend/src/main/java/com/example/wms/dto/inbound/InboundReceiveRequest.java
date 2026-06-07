package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class InboundReceiveRequest {

    @Valid
    @NotEmpty(message = "入库明细不能为空")
    private List<InboundReceiveDetailRequest> details;
}
