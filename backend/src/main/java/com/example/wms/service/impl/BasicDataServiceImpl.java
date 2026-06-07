package com.example.wms.service.impl;

import com.example.wms.dto.basic.MaterialOptionDTO;
import com.example.wms.dto.basic.PackagingInfoDTO;
import com.example.wms.dto.basic.SupplierOptionDTO;
import com.example.wms.entity.MaterialInfo;
import com.example.wms.entity.PackagingInfo;
import com.example.wms.entity.SupplierInfo;
import com.example.wms.repository.MaterialInfoRepository;
import com.example.wms.repository.PackagingInfoRepository;
import com.example.wms.repository.SupplierInfoRepository;
import com.example.wms.service.BasicDataService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BasicDataServiceImpl implements BasicDataService {

    private final SupplierInfoRepository supplierInfoRepository;
    private final MaterialInfoRepository materialInfoRepository;
    private final PackagingInfoRepository packagingInfoRepository;

    public BasicDataServiceImpl(SupplierInfoRepository supplierInfoRepository,
                                MaterialInfoRepository materialInfoRepository,
                                PackagingInfoRepository packagingInfoRepository) {
        this.supplierInfoRepository = supplierInfoRepository;
        this.materialInfoRepository = materialInfoRepository;
        this.packagingInfoRepository = packagingInfoRepository;
    }

    @Override
    public List<SupplierOptionDTO> listSuppliers() {
        return supplierInfoRepository.findAllByOrderBySupplierCodeAsc().stream()
                .map(item -> new SupplierOptionDTO(
                        item.getId(),
                        item.getSupplierCode(),
                        item.getSupplierName(),
                        item.getContact(),
                        item.getPhone()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<MaterialOptionDTO> listMaterials(String supplierCode) {
        List<MaterialInfo> materials = supplierCode == null || supplierCode.trim().isEmpty()
                ? materialInfoRepository.findAllByOrderByMaterialNoAsc()
                : materialInfoRepository.findBySupplierCodeOrderByMaterialNoAsc(supplierCode.trim());

        Map<String, PackagingInfo> packagingMap = packagingInfoRepository.findAllByOrderByMaterialNoAsc().stream()
                .collect(Collectors.toMap(PackagingInfo::getMaterialNo, item -> item, (left, right) -> left));

        return materials.stream()
                .map(item -> {
                    PackagingInfo packagingInfo = packagingMap.get(item.getMaterialNo());
                    return new MaterialOptionDTO(
                            item.getId(),
                            item.getMaterialNo(),
                            item.getMaterialName(),
                            item.getMaterialType(),
                            item.getUnit(),
                            item.getSupplierCode(),
                            item.getSupplierName(),
                            packagingInfo == null ? null : packagingInfo.getPackageCapacity(),
                            packagingInfo == null ? null : packagingInfo.getPackageModel()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PackagingInfoDTO> listPackagingInfos() {
        return packagingInfoRepository.findAllByOrderByMaterialNoAsc().stream()
                .map(item -> new PackagingInfoDTO(
                        item.getId(),
                        item.getMaterialNo(),
                        item.getSupplierCode(),
                        item.getPackageModel(),
                        item.getPackageCapacity()
                ))
                .collect(Collectors.toList());
    }
}
