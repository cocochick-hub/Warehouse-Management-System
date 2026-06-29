package com.example.wms.repository;

import com.example.wms.entity.InventoryCheckDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCheckDetailRepo extends JpaRepository<InventoryCheckDetail, Long>, JpaSpecificationExecutor<InventoryCheckDetail> {

    List<InventoryCheckDetail> findByTaskId(Long taskId);

    List<InventoryCheckDetail> findByTaskNo(String taskNo);

    /** 按任务ID+物料编码+库区查询明细（用于扫码场景） */
    Optional<InventoryCheckDetail> findByTaskIdAndMaterialCodeAndWarehouseArea(Long taskId, String materialCode, String warehouseArea);

    /** 按任务ID+物料编码查询明细（兼容不区分库区的场景） */
    Optional<InventoryCheckDetail> findByTaskIdAndMaterialCode(Long taskId, String materialCode);

    int countByTaskId(Long taskId);

    int countByTaskIdAndStatus(Long taskId, String status);
}