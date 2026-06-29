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

    // ==================== 供应商管理 ====================

    @Override
    public List<SupplierOptionDTO> listSuppliers() {
        return supplierInfoRepository.findAllByOrderBySupplierCodeAsc().stream()
                .map(this::toSupplierDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierOptionDTO createSupplier(SupplierOptionDTO dto) {
        SupplierInfo entity = new SupplierInfo();
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setSupplierName(dto.getSupplierName());
        entity.setContact(dto.getContact());
        entity.setPhone(dto.getPhone());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toSupplierDTO(supplierInfoRepository.save(entity));
    }

    @Override
    public SupplierOptionDTO updateSupplier(Long id, SupplierOptionDTO dto) {
        SupplierInfo entity = supplierInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("供应商不存在"));
        entity.setSupplierName(dto.getSupplierName());
        entity.setContact(dto.getContact());
        entity.setPhone(dto.getPhone());
        return toSupplierDTO(supplierInfoRepository.save(entity));
    }

    @Override
    public void deleteSupplier(Long id) {
        if (!supplierInfoRepository.existsById(id)) {
            throw new EntityNotFoundException("供应商不存在");
        }
        supplierInfoRepository.deleteById(id);
    }

    // ==================== 物料管理 ====================

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
    public MaterialOptionDTO createMaterial(MaterialOptionDTO dto) {
        MaterialInfo entity = new MaterialInfo();
        entity.setMaterialNo(dto.getMaterialNo());
        entity.setMaterialName(dto.getMaterialName());
        entity.setMaterialType(dto.getMaterialType());
        entity.setUnit(dto.getUnit());
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setSupplierName(dto.getSupplierName());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        MaterialInfo saved = materialInfoRepository.save(entity);
        return toMaterialDTO(saved);
    }

    @Override
    public MaterialOptionDTO updateMaterial(Long id, MaterialOptionDTO dto) {
        MaterialInfo entity = materialInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("物料不存在"));
        entity.setMaterialName(dto.getMaterialName());
        entity.setMaterialType(dto.getMaterialType());
        entity.setUnit(dto.getUnit());
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setSupplierName(dto.getSupplierName());
        MaterialInfo saved = materialInfoRepository.save(entity);
        return toMaterialDTO(saved);
    }

    @Override
    public void deleteMaterial(Long id) {
        if (!materialInfoRepository.existsById(id)) {
            throw new EntityNotFoundException("物料不存在");
        }
        materialInfoRepository.deleteById(id);
    }

    // ==================== 包装管理 ====================

    @Override
    public List<PackagingInfoDTO> listPackagingInfos() {
        return packagingInfoRepository.findAllByOrderByMaterialNoAsc().stream()
                .map(this::toPackagingDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PackagingInfoDTO createPackaging(PackagingInfoDTO dto) {
        PackagingInfo entity = new PackagingInfo();
        entity.setMaterialNo(dto.getMaterialNo());
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setPackageModel(dto.getPackageModel());
        entity.setPackageCapacity(dto.getPackageCapacity());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toPackagingDTO(packagingInfoRepository.save(entity));
    }

    @Override
    public PackagingInfoDTO updatePackaging(Long id, PackagingInfoDTO dto) {
        PackagingInfo entity = packagingInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("包装信息不存在"));
        entity.setPackageModel(dto.getPackageModel());
        entity.setPackageCapacity(dto.getPackageCapacity());
        return toPackagingDTO(packagingInfoRepository.save(entity));
    }

    @Override
    public void deletePackaging(Long id) {
        if (!packagingInfoRepository.existsById(id)) {
            throw new EntityNotFoundException("包装信息不存在");
        }
        packagingInfoRepository.deleteById(id);
    }

    // ==================== 库区管理 ====================

    @Override
    public List<WarehouseAreaDTO> listWarehouseAreas() {
        return warehouseAreaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toWarehouseAreaDTO)
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
        return toWarehouseAreaDTO(warehouseAreaRepository.save(entity));
    }

    @Override
    public WarehouseAreaDTO updateWarehouseArea(Long id, WarehouseAreaDTO dto) {
        WarehouseArea entity = warehouseAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("库区不存在"));
        entity.setAreaName(dto.getAreaName());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setDescription(dto.getDescription());
        return toWarehouseAreaDTO(warehouseAreaRepository.save(entity));
    }

    @Override
    public void deleteWarehouseArea(Long id) {
        if (!warehouseAreaRepository.existsById(id)) {
            throw new EntityNotFoundException("库区不存在");
        }
        warehouseAreaRepository.deleteById(id);
    }

    // ==================== DTO 转换 ====================

    private SupplierOptionDTO toSupplierDTO(SupplierInfo entity) {
        return new SupplierOptionDTO(
                entity.getId(),
                entity.getSupplierCode(),
                entity.getSupplierName(),
                entity.getContact(),
                entity.getPhone()
        );
    }

    private MaterialOptionDTO toMaterialDTO(MaterialInfo entity) {
        return new MaterialOptionDTO(
                entity.getId(),
                entity.getMaterialNo(),
                entity.getMaterialName(),
                entity.getMaterialType(),
                entity.getUnit(),
                entity.getSupplierCode(),
                entity.getSupplierName(),
                null,
                null
        );
    }

    private PackagingInfoDTO toPackagingDTO(PackagingInfo entity) {
        return new PackagingInfoDTO(
                entity.getId(),
                entity.getMaterialNo(),
                entity.getSupplierCode(),
                entity.getPackageModel(),
                entity.getPackageCapacity()
        );
    }

    private WarehouseAreaDTO toWarehouseAreaDTO(WarehouseArea entity) {
        return new WarehouseAreaDTO(
                entity.getId(),
                entity.getAreaCode(),
                entity.getAreaName(),
                entity.getSortOrder(),
                entity.getDescription()
        );
    }
}