package com.example.wms.repository;

import com.example.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    List<InboundOrder> findByStatusNotOrderByCreatedAtDesc(String status);
}
