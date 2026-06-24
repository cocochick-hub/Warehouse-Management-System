package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 退库请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundReturnRequest {

    @NotBlank(message = "看板号不能为空")
    @Size(max = 255, message = "看板号长度不能超过255")
    private String kanbanNo;
}
