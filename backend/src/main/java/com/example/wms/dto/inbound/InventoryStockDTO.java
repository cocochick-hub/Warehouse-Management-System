package com.example.wms.dto.inbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockDTO {

    private String materialCode;
    private String materialName;
    private String supplier;
    private Integer onHandQty;
    private String lastInboundDocNo;
    private LocalDateTime lastInboundAt;
}
