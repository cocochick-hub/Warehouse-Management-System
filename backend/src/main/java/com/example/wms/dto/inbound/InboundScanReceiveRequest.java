package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class InboundScanReceiveRequest {

    @NotBlank(message = "看板号不能为空")
    @Size(max = 255, message = "看板号长度不能超过255")
    private String kanbanNo;
}
