package com.example.wms.repository;

import com.example.wms.entity.DemandDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 需求明细 Repository */
@Repository
public interface DemandDetailRepository extends JpaRepository<DemandDetail, Long>, JpaSpecificationExecutor<DemandDetail> {

    List<DemandDetail> findByBatchNoOrderByCreatedAtAsc(String batchNo);

    List<DemandDetail> findByBatchIdOrderByCreatedAtAsc(Long batchId);
}
