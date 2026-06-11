package com.example.wms.dto.inventory;

import com.example.wms.dto.inbound.InventoryStockDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockPageResponse {

    private Integer total;
    private Integer page;
    private Integer size;
    private List<InventoryStockDTO> records;
}
