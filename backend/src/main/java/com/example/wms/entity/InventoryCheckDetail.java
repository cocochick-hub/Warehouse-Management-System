package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_check_detail")
public class InventoryCheckDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_no", nullable = false, length = 50)
    private String taskNo;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(nullable = false, length = 100)
    private String supplier;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    @Column(name = "system_qty", nullable = false)
    private Integer systemQty = 0;

    @Column(name = "actual_qty")
    private Integer actualQty;

    @Column(name = "diff_qty")
    private Integer diffQty;

    @Column(nullable = false, length = 20)
    private String status = "待盘";

    @Column(name = "checked_by", length = 50)
    private String checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}