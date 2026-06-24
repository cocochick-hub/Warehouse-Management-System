package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.basic.MaterialOptionDTO;
import com.example.wms.dto.basic.PackagingInfoDTO;
import com.example.wms.dto.basic.SupplierOptionDTO;
import com.example.wms.dto.basic.WarehouseAreaDTO;
import com.example.wms.service.BasicDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic")
public class BasicDataController {

    private final BasicDataService basicDataService;

    public BasicDataController(BasicDataService basicDataService) {
        this.basicDataService = basicDataService;
    }

    @GetMapping("/suppliers")
    public ApiResult<List<SupplierOptionDTO>> listSuppliers() {
        return ApiResult.success(basicDataService.listSuppliers());
    }

    @GetMapping("/materials")
    public ApiResult<List<MaterialOptionDTO>> listMaterials(@RequestParam(required = false) String supplierCode) {
        return ApiResult.success(basicDataService.listMaterials(supplierCode));
    }

    @GetMapping("/packaging")
    public ApiResult<List<PackagingInfoDTO>> listPackagingInfos() {
        return ApiResult.success(basicDataService.listPackagingInfos());
    }

    // ==================== 库区管理 ====================

    @GetMapping("/warehouse-areas")
    public ApiResult<List<WarehouseAreaDTO>> listWarehouseAreas() {
        return ApiResult.success(basicDataService.listWarehouseAreas());
    }

    @PostMapping("/warehouse-areas")
    public ApiResult<WarehouseAreaDTO> createWarehouseArea(@RequestBody WarehouseAreaDTO dto) {
        return ApiResult.success(basicDataService.createWarehouseArea(dto));
    }

    @PutMapping("/warehouse-areas/{id}")
    public ApiResult<WarehouseAreaDTO> updateWarehouseArea(@PathVariable Long id, @RequestBody WarehouseAreaDTO dto) {
        return ApiResult.success(basicDataService.updateWarehouseArea(id, dto));
    }

    @DeleteMapping("/warehouse-areas/{id}")
    public ApiResult<Void> deleteWarehouseArea(@PathVariable Long id) {
        basicDataService.deleteWarehouseArea(id);
        return ApiResult.success(null);
    }
}
