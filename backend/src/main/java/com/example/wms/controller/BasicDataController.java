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

    // ==================== 供应商管理 ====================

    @GetMapping("/suppliers")
    public ApiResult<List<SupplierOptionDTO>> listSuppliers() {
        return ApiResult.success(basicDataService.listSuppliers());
    }

    @PostMapping("/suppliers")
    public ApiResult<SupplierOptionDTO> createSupplier(@RequestBody SupplierOptionDTO dto) {
        return ApiResult.success(basicDataService.createSupplier(dto));
    }

    @PutMapping("/suppliers/{id}")
    public ApiResult<SupplierOptionDTO> updateSupplier(@PathVariable Long id, @RequestBody SupplierOptionDTO dto) {
        return ApiResult.success(basicDataService.updateSupplier(id, dto));
    }

    @DeleteMapping("/suppliers/{id}")
    public ApiResult<Void> deleteSupplier(@PathVariable Long id) {
        basicDataService.deleteSupplier(id);
        return ApiResult.success(null);
    }

    // ==================== 物料管理 ====================

    @GetMapping("/materials")
    public ApiResult<List<MaterialOptionDTO>> listMaterials(@RequestParam(required = false) String supplierCode) {
        return ApiResult.success(basicDataService.listMaterials(supplierCode));
    }

    @PostMapping("/materials")
    public ApiResult<MaterialOptionDTO> createMaterial(@RequestBody MaterialOptionDTO dto) {
        return ApiResult.success(basicDataService.createMaterial(dto));
    }

    @PutMapping("/materials/{id}")
    public ApiResult<MaterialOptionDTO> updateMaterial(@PathVariable Long id, @RequestBody MaterialOptionDTO dto) {
        return ApiResult.success(basicDataService.updateMaterial(id, dto));
    }

    @DeleteMapping("/materials/{id}")
    public ApiResult<Void> deleteMaterial(@PathVariable Long id) {
        basicDataService.deleteMaterial(id);
        return ApiResult.success(null);
    }

    // ==================== 包装管理 ====================

    @GetMapping("/packaging")
    public ApiResult<List<PackagingInfoDTO>> listPackagingInfos() {
        return ApiResult.success(basicDataService.listPackagingInfos());
    }

    @PostMapping("/packaging")
    public ApiResult<PackagingInfoDTO> createPackaging(@RequestBody PackagingInfoDTO dto) {
        return ApiResult.success(basicDataService.createPackaging(dto));
    }

    @PutMapping("/packaging/{id}")
    public ApiResult<PackagingInfoDTO> updatePackaging(@PathVariable Long id, @RequestBody PackagingInfoDTO dto) {
        return ApiResult.success(basicDataService.updatePackaging(id, dto));
    }

    @DeleteMapping("/packaging/{id}")
    public ApiResult<Void> deletePackaging(@PathVariable Long id) {
        basicDataService.deletePackaging(id);
        return ApiResult.success(null);
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