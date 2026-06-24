package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 物料需求批次表
 */
@Getter
@Setter
@Entity
@Table(name = "demand_batch")
public class DemandBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 需求批次号 */
    @Column(name = "batch_no", nullable = false, length = 50)
    private String batchNo;

    /** 物料种类数 */
    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    /** 需求总数量 */
    @Column(name = "total_qty", nullable = false)
    private Integer totalQty;

    /** 录入方式：MANUAL(手工) / EXCEL(导入) */
    @Column(name = "import_type", nullable = false, length = 20)
    private String importType;

    /** 操作人 */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
