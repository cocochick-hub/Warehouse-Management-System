package com.example.wms.dto.outbound;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OutboundScanIssueRequest {

    @NotBlank(message = "看板号不能为空")
    private String kanbanNo;

    @NotNull(message = "出库数量不能为空")
    private Integer issueQty;

    private Long outboundOrderId;

    private String warehouseArea;
}
