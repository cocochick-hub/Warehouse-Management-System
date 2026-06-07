package com.example.wms.dto.inbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundOrderPageResponse {

    private long total;
    private int page;
    private int size;
    private List<InboundOrderSummaryDTO> records;
}
