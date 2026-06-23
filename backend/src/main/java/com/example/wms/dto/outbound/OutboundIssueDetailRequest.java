package com.example.wms.dto.outbound;

import lombok.Data;

@Data
public class OutboundIssueDetailRequest {

    private Long detailId;
    private Integer issueQty;
}
