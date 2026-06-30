package com.example.wms.repository;

import com.example.wms.entity.InboundOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InboundOrderDetailRepository extends JpaRepository<InboundOrderDetail, Long>, JpaSpecificationExecutor<InboundOrderDetail> {

    List<InboundOrderDetail> findByInboundOrderIdOrderByLineNoAsc(Long inboundOrderId);

    List<InboundOrderDetail> findByDocNoOrderByLineNoAsc(String docNo);

    List<InboundOrderDetail> findByInboundOrderIdIn(Collection<Long> inboundOrderIds);

    List<InboundOrderDetail> findByMaterialCodeAndSupplierName(String materialCode, String supplierName);

    /**
     * 查询指定物料+供应商的最早已完成/部分完成入库单号
     * 用 JOIN 一次查询替代 N+1，性能远优于逐条查
     */
    @org.springframework.data.jpa.repository.Query("SELECT o.docNo FROM InboundOrder o, InboundOrderDetail d "
            + "WHERE o.docNo = d.docNo AND d.materialCode = :materialCode AND d.supplierName = :supplierName "
            + "AND (o.status = '已完成' OR o.status = '部分完成') "
            + "ORDER BY o.createdAt ASC")
    List<String> findEarliestCompletedDocNo(@org.springframework.data.repository.query.Param("materialCode") String materialCode,
                                            @org.springframework.data.repository.query.Param("supplierName") String supplierName,
                                            org.springframework.data.domain.Pageable pageable);
}
