package com.example.wms.repository;

import com.example.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long>, JpaSpecificationExecutor<InboundOrder> {

    List<InboundOrder> findByStatusNotOrderByCreatedAtDesc(String status);

    Optional<InboundOrder> findByDocNo(String docNo);

    boolean existsByDocNo(String docNo);
}
