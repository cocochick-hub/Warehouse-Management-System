package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.inventory.InventoryStockPageResponse;
import com.example.wms.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/stocks")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResult<InventoryStockPageResponse> listStocks(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String transferStatus,
            @RequestParam(required = false) String warehouseArea,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResult.success(inventoryService.listStocks(materialCode, materialName, supplier, transferStatus, warehouseArea, page, size));
    }

    /** 根据物料号和供应商查看看板标签列表 */
    @GetMapping("/labels")
    public ApiResult<List<InboundKanbanLabelDTO>> listLabels(
            @RequestParam String materialCode,
            @RequestParam String supplier,
            @RequestParam(required = false) String warehouseArea) {
        return ApiResult.success(inventoryService.listLabelsByMaterial(materialCode, supplier, warehouseArea));
    }
}
