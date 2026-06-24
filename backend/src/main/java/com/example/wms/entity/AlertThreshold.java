package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 高低储预警阈值配置
 */
@Getter
@Setter
@Entity
@Table(name = "alert_threshold", uniqueConstraints = {
        @UniqueConstraint(name = "uk_alert_threshold_material_supplier",
                columnNames = {"material_code", "supplier"})
})
public class AlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(nullable = false, length = 100)
    private String supplier;

    @Column(name = "low_stock_qty", nullable = false)
    private Integer lowStockQty;

    @Column(name = "high_stock_qty", nullable = false)
    private Integer highStockQty;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
