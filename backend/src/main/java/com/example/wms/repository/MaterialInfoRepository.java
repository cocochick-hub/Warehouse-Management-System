package com.example.wms.repository;

import com.example.wms.entity.MaterialInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialInfoRepository extends JpaRepository<MaterialInfo, Long> {

    List<MaterialInfo> findAllByOrderByMaterialNoAsc();

    List<MaterialInfo> findBySupplierCodeOrderByMaterialNoAsc(String supplierCode);
}
