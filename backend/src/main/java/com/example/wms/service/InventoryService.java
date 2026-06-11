package com.example.wms.service;

import com.example.wms.dto.inventory.InventoryStockPageResponse;

public interface InventoryService {

    InventoryStockPageResponse listStocks(String materialCode, String materialName, String supplier, Integer page, Integer size);
}
