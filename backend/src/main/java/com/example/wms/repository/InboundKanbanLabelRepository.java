package com.example.wms.repository;

import com.example.wms.entity.InboundKanbanLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InboundKanbanLabelRepository extends JpaRepository<InboundKanbanLabel, Long> {

    List<InboundKanbanLabel> findByInboundOrderIdOrderByInboundOrderDetailIdAscPackageSeqAsc(Long inboundOrderId);

    List<InboundKanbanLabel> findByInboundOrderDetailIdIn(Collection<Long> detailIds);

    Optional<InboundKanbanLabel> findByKanbanNo(String kanbanNo);

    boolean existsByKanbanNo(String kanbanNo);

    List<InboundKanbanLabel> findByMaterialCodeAndSupplierNameOrderByCreatedAtAsc(String materialCode, String supplierName);

    /** 查询指定物料+供应商下所有已封存的看板标签 */
    List<InboundKanbanLabel> findByMaterialCodeAndSupplierNameAndSealedTrue(String materialCode, String supplierName);

    /** 查询所有已封存的看板标签 */
    List<InboundKanbanLabel> findBySealedTrue();
}
