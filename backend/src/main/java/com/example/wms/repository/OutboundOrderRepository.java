package com.example.wms.repository;

import com.example.wms.entity.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {

    List<OutboundOrder> findByStatusNotOrderByCreatedAtDesc(String status);
}
