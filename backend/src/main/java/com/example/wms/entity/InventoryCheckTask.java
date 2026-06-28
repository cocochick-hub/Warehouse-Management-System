package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_check_task")
public class InventoryCheckTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, unique = true, length = 50)
    private String taskNo;

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @Column(name = "check_type", nullable = false, length = 20)
    private String checkType = "明盘";

    @Column(nullable = false, length = 20)
    private String status = "进行中";

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    @Column(name = "material_code", length = 50)
    private String materialCode;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
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