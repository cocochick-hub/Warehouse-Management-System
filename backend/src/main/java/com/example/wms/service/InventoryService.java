package com.example.wms.service;

import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.inventory.InventoryStockPageResponse;

import java.util.List;

public interface InventoryService {

    InventoryStockPageResponse listStocks(String materialCode, String materialName, String supplier, String transferStatus, String warehouseArea, Integer page, Integer size);

    List<InboundKanbanLabelDTO> listLabelsByMaterial(String materialCode, String supplier, String warehouseArea);
}
