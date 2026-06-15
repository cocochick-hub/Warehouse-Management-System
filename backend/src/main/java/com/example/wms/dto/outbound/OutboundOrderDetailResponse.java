package com.example.wms.dto.outbound;

import com.example.wms.dto.inbound.InventoryStockDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundOrderDetailResponse {

    private OutboundOrderSummaryDTO order;
    private List<OutboundOrderDetailDTO> details;
    private List<InventoryStockDTO> stocks;
}
