package com.example.wms.repository;

import com.example.wms.entity.OutboundHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboundHistoryRepository extends JpaRepository<OutboundHistory, Long>, JpaSpecificationExecutor<OutboundHistory> {

    List<OutboundHistory> findByDocNoOrderByCreatedAtDesc(String docNo);

    List<OutboundHistory> findBySourceDetailId(Long sourceDetailId);

    List<OutboundHistory> findByOutboundDetailId(Long outboundDetailId);

    /** AI智能预警：查询指定时间之后的出库记录 */
    List<OutboundHistory> findByCreatedAtAfter(LocalDateTime dateTime);
}
