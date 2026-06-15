package com.example.wms.dto.outbound;

import lombok.Data;

import java.util.List;

@Data
public class OutboundIssueRequest {

    private List<OutboundIssueDetailRequest> details;
}
