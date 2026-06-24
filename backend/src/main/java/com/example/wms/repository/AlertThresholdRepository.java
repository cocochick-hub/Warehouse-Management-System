package com.example.wms.repository;

import com.example.wms.entity.AlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 高低储预警阈值 Repository */
@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, Long> {

    Optional<AlertThreshold> findByMaterialCodeAndSupplier(String materialCode, String supplier);
}
