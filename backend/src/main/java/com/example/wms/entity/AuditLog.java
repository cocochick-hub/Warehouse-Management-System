package com.example.wms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "target", nullable = false, length = 100)
    private String target;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
