package com.example.wms.repository;

import com.example.wms.entity.DemandBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 需求批次 Repository */
@Repository
public interface DemandBatchRepository extends JpaRepository<DemandBatch, Long> {

    Optional<DemandBatch> findByBatchNo(String batchNo);
}
