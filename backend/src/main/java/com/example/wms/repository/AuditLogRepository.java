package com.example.wms.repository;

import com.example.wms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsernameContainingAndActionContainingAndCreatedAtBetweenOrderByCreatedAtDesc(
            String username, String action, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);
}
