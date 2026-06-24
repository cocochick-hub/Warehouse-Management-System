package com.example.wms.service.impl;

import com.example.wms.dto.basic.MaterialOptionDTO;
import com.example.wms.dto.basic.PackagingInfoDTO;
import com.example.wms.dto.basic.SupplierOptionDTO;
import com.example.wms.dto.basic.WarehouseAreaDTO;
import com.example.wms.entity.MaterialInfo;
import com.example.wms.entity.PackagingInfo;
import com.example.wms.entity.SupplierInfo;
import com.example.wms.entity.WarehouseArea;
import com.example.wms.repository.MaterialInfoRepository;
import com.example.wms.repository.PackagingInfoRepository;
import com.example.wms.repository.SupplierInfoRepository;
import com.example.wms.repository.WarehouseAreaRepository;
import com.example.wms.service.BasicDataService;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BasicDataServiceImpl implements BasicDataService {

    private final SupplierInfoRepository supplierInfoRepository;
    private final MaterialInfoRepository materialInfoRepository;
    private final PackagingInfoRepository packagingInfoRepository;
    private final WarehouseAreaRepository warehouseAreaRepository;

    public BasicDataServiceImpl(SupplierInfoRepository supplierInfoRepository,
                                MaterialInfoRepository materialInfoRepository,
                                PackagingInfoRepository packagingInfoRepository,
                                WarehouseAreaRepository warehouseAreaRepository) {
        this.supplierInfoRepository = supplierInfoRepository;
        this.materialInfoRepository = materialInfoRepository;
        this.packagingInfoRepository = packagingInfoRepository;
        this.warehouseAreaRepository = warehouseAreaRepository;
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

    @Override
    public List<WarehouseAreaDTO> listWarehouseAreas() {
        return warehouseAreaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(item -> new WarehouseAreaDTO(
                        item.getId(),
                        item.getAreaCode(),
                        item.getAreaName(),
                        item.getSortOrder(),
                        item.getDescription()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseAreaDTO createWarehouseArea(WarehouseAreaDTO dto) {
        WarehouseArea entity = new WarehouseArea();
        entity.setAreaCode(dto.getAreaCode());
        entity.setAreaName(dto.getAreaName());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setDescription(dto.getDescription());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        WarehouseArea saved = warehouseAreaRepository.save(entity);
        return toDTO(saved);
    }

    @Override
    public WarehouseAreaDTO updateWarehouseArea(Long id, WarehouseAreaDTO dto) {
        WarehouseArea entity = warehouseAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("库区不存在"));
        entity.setAreaName(dto.getAreaName());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setDescription(dto.getDescription());
        WarehouseArea saved = warehouseAreaRepository.save(entity);
        return toDTO(saved);
    }

    @Override
    public void deleteWarehouseArea(Long id) {
        if (!warehouseAreaRepository.existsById(id)) {
            throw new EntityNotFoundException("库区不存在");
        }
        warehouseAreaRepository.deleteById(id);
    }

    private WarehouseAreaDTO toDTO(WarehouseArea entity) {
        return new WarehouseAreaDTO(
                entity.getId(),
                entity.getAreaCode(),
                entity.getAreaName(),
                entity.getSortOrder(),
                entity.getDescription()
        );
    }
}
