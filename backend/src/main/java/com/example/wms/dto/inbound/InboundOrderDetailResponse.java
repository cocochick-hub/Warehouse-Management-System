package com.example.wms.dto.inbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundOrderDetailResponse {

    private InboundOrderSummaryDTO order;
    private List<InboundOrderDetailDTO> details;
}
