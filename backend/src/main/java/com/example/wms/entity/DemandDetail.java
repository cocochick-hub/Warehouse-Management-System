package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物料需求明细表
 */
@Getter
@Setter
@Entity
@Table(name = "demand_detail")
public class DemandDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 需求批次ID */
    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    /** 需求批次号冗余 */
    @Column(name = "batch_no", nullable = false, length = 50)
    private String batchNo;

    /** 物料号 */
    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    /** 物料名称快照 */
    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    /** 供应商代码快照 */
    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    /** 供应商名称快照 */
    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    /** 需求数量 */
    @Column(name = "demand_qty", nullable = false)
    private Integer demandQty;

    /** 已满足数量 */
    @Column(name = "fulfilled_qty", nullable = false)
    private Integer fulfilledQty;

    /** 需求日期 */
    @Column(name = "demand_date")
    private LocalDate demandDate;

    /** 期望库区 */
    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    /** 状态：待出库 / 部分完成 / 已完成 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 备注 */
    @Column(name = "remark", length = 255)
    private String remark;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
