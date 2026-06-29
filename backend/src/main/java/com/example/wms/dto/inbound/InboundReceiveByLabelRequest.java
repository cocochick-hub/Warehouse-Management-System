package com.example.wms.dto.inbound;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class InboundReceiveByLabelRequest {

    @NotEmpty(message = "请选择待入库的看板")
    private List<Long> labelIds;
}
