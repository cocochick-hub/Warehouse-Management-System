package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class InboundReceiveDetailRequest {

    @NotNull(message = "明细ID不能为空")
    private Long detailId;

    @NotNull(message = "本次入库数量不能为空")
    @Min(value = 0, message = "本次入库数量不能小于0")
    private Integer receiveQty;
}
