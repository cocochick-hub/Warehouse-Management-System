package com.example.wms.repository;

import com.example.wms.entity.OutboundHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboundHistoryRepository extends JpaRepository<OutboundHistory, Long>, JpaSpecificationExecutor<OutboundHistory> {

    List<OutboundHistory> findByDocNoOrderByCreatedAtDesc(String docNo);

    List<OutboundHistory> findBySourceDetailId(Long sourceDetailId);

    List<OutboundHistory> findByOutboundDetailId(Long outboundDetailId);
}
