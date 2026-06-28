package com.example.wms.repository;

import com.example.wms.entity.PackageTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageTransferRepository extends JpaRepository<PackageTransfer, Long> {

    /** 按源看板号查询转包历史 */
    List<PackageTransfer> findBySourceKanbanNoOrderByCreatedAtDesc(String sourceKanbanNo);

    /** 按目标看板号查询转包历史 */
    List<PackageTransfer> findByTargetKanbanNoOrderByCreatedAtDesc(String targetKanbanNo);
}
