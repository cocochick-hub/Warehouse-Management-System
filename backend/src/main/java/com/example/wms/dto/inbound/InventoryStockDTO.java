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
    private String transferStatus;
    private String warehouseArea;
    private Integer availableQty;

    public InventoryStockDTO(String materialCode, String materialName, String supplier, Integer onHandQty,
                             String lastInboundDocNo, LocalDateTime lastInboundAt,
                             String transferStatus, String warehouseArea) {
        this(materialCode, materialName, supplier, onHandQty, lastInboundDocNo, lastInboundAt,
                transferStatus, warehouseArea, onHandQty);
    }
}
