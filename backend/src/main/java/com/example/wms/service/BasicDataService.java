package com.example.wms.service;

import com.example.wms.dto.basic.MaterialOptionDTO;
import com.example.wms.dto.basic.PackagingInfoDTO;
import com.example.wms.dto.basic.SupplierOptionDTO;

import java.util.List;

public interface BasicDataService {

    List<SupplierOptionDTO> listSuppliers();

    List<MaterialOptionDTO> listMaterials(String supplierCode);

    List<PackagingInfoDTO> listPackagingInfos();
}
