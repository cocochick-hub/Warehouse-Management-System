package com.example.wms.dto.outbound;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

@Data
public class OutboundIssueByLabelRequest {

    @NotEmpty(message = "请选择要出库的看板")
    private List<Long> labelIds;

    /**
     * 可选：每个标签的出库数量，key=labelId，value=出库数量。
     * 如果不传，则默认按标签的全部 labelQty 出库。
     */
    private Map<Long, Integer> labelIssueQtys;
}
