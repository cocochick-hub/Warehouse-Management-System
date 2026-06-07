package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.basic.MaterialOptionDTO;
import com.example.wms.dto.basic.PackagingInfoDTO;
import com.example.wms.dto.basic.SupplierOptionDTO;
import com.example.wms.service.BasicDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
