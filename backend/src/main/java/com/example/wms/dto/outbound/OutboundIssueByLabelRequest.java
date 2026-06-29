package com.example.wms.dto.outbound;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class OutboundIssueByLabelRequest {

    @NotEmpty(message = "请选择要出库的看板")
    private List<Long> labelIds;
}
