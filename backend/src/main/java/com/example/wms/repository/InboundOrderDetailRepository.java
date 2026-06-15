package com.example.wms.repository;

import com.example.wms.entity.InboundOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboundOrderDetailRepository extends JpaRepository<InboundOrderDetail, Long> {

    List<InboundOrderDetail> findByInboundOrderIdOrderByLineNoAsc(Long inboundOrderId);

    List<InboundOrderDetail> findByDocNoOrderByLineNoAsc(String docNo);

    List<InboundOrderDetail> findByMaterialCodeAndSupplierName(String materialCode, String supplierName);
}
