package com.example.wms.repository;

import com.example.wms.entity.InventoryCheckTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCheckTaskRepo extends JpaRepository<InventoryCheckTask, Long>, JpaSpecificationExecutor<InventoryCheckTask> {

    Optional<InventoryCheckTask> findByTaskNo(String taskNo);

    List<InventoryCheckTask> findByStatusOrderByCreatedAtDesc(String status);

    List<InventoryCheckTask> findAllByOrderByCreatedAtDesc();

    /** 查询今日最新的盘点单号序号 */
    Optional<InventoryCheckTask> findTopByTaskNoStartingWithOrderByTaskNoDesc(String prefix);
}