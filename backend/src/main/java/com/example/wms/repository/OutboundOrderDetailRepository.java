package com.example.wms.repository;

import com.example.wms.entity.OutboundOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboundOrderDetailRepository extends JpaRepository<OutboundOrderDetail, Long> {

    List<OutboundOrderDetail> findByOutboundOrderIdOrderByLineNoAsc(Long outboundOrderId);

    List<OutboundOrderDetail> findByDocNoOrderByLineNoAsc(String docNo);
}
