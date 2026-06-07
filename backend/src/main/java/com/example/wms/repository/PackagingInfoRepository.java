package com.example.wms.repository;

import com.example.wms.entity.PackagingInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackagingInfoRepository extends JpaRepository<PackagingInfo, Long> {

    List<PackagingInfo> findAllByOrderByMaterialNoAsc();
}
