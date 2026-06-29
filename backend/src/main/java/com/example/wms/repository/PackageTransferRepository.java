package com.example.wms.repository;

import com.example.wms.entity.PackageTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageTransferRepository extends JpaRepository<PackageTransfer, Long> {

    /** 按源看板号查询转包历史 */
    List<PackageTransfer> findBySourceKanbanNoOrderByCreatedAtDesc(String sourceKanbanNo);

    /** 按目标看板号查询转包历史 */
    List<PackageTransfer> findByTargetKanbanNoOrderByCreatedAtDesc(String targetKanbanNo);

    /** 分页查询转包历史（无条件） */
    Page<PackageTransfer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 分页查询转包历史（按源看板号筛选） */
    Page<PackageTransfer> findBySourceKanbanNoOrderByCreatedAtDesc(String sourceKanbanNo, Pageable pageable);

    /** 分页查询转包历史（按目标看板号筛选） */
    Page<PackageTransfer> findByTargetKanbanNoOrderByCreatedAtDesc(String targetKanbanNo, Pageable pageable);

    /** 分页查询转包历史（按源看板号和目标看板号筛选） */
    Page<PackageTransfer> findBySourceKanbanNoContainingAndTargetKanbanNoContainingOrderByCreatedAtDesc(
            String sourceKanbanNo, String targetKanbanNo, Pageable pageable);

    /** 查询指定看板作为源看板的历史转出总量 */
    @Query("SELECT COALESCE(SUM(p.transferQty), 0) FROM PackageTransfer p WHERE p.sourceKanbanNo = :kanbanNo")
    Integer sumTransferQtyBySourceKanbanNo(@Param("kanbanNo") String sourceKanbanNo);
}
