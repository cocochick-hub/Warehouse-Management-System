package com.example.wms.repository;

import com.example.wms.entity.SupplierInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierInfoRepository extends JpaRepository<SupplierInfo, Long> {

    List<SupplierInfo> findAllByOrderBySupplierCodeAsc();
}
