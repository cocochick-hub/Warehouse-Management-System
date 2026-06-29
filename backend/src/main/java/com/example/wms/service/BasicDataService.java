package com.example.wms.service;

import com.example.wms.dto.basic.MaterialOptionDTO;
import com.example.wms.dto.basic.PackagingInfoDTO;
import com.example.wms.dto.basic.SupplierOptionDTO;
import com.example.wms.dto.basic.WarehouseAreaDTO;

import java.util.List;

public interface BasicDataService {

    List<SupplierOptionDTO> listSuppliers();

    SupplierOptionDTO createSupplier(SupplierOptionDTO dto);

    SupplierOptionDTO updateSupplier(Long id, SupplierOptionDTO dto);

    void deleteSupplier(Long id);

    List<MaterialOptionDTO> listMaterials(String supplierCode);

    MaterialOptionDTO createMaterial(MaterialOptionDTO dto);

    MaterialOptionDTO updateMaterial(Long id, MaterialOptionDTO dto);

    void deleteMaterial(Long id);

    List<PackagingInfoDTO> listPackagingInfos();

    PackagingInfoDTO createPackaging(PackagingInfoDTO dto);

    PackagingInfoDTO updatePackaging(Long id, PackagingInfoDTO dto);

    void deletePackaging(Long id);

    List<WarehouseAreaDTO> listWarehouseAreas();

    WarehouseAreaDTO createWarehouseArea(WarehouseAreaDTO dto);

    WarehouseAreaDTO updateWarehouseArea(Long id, WarehouseAreaDTO dto);

    void deleteWarehouseArea(Long id);
}
