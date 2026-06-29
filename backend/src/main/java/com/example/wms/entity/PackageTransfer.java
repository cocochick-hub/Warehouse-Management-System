package com.example.wms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 转包操作记录实体
 * 记录每一次看板间物料转移的源和目标信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "package_transfer")
public class PackageTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 源看板号 */
    @Column(name = "source_kanban_no", nullable = false)
    private String sourceKanbanNo;

    /** 目标看板号（新生成的看板号） */
    @Column(name = "target_kanban_no", nullable = false)
    private String targetKanbanNo;

    /** 转移数量 */
    @Column(name = "transfer_qty", nullable = false)
    private Integer transferQty;

    /** 转移前源看板可用数量 */
    @Column(name = "source_qty_before", nullable = false)
    private Integer sourceQtyBefore;

    /** 转移后源看板可用数量 */
    @Column(name = "source_qty_after", nullable = false)
    private Integer sourceQtyAfter;

    /** 物料编码快照 */
    @Column(name = "material_code", nullable = false)
    private String materialCode;

    /** 物料名称快照 */
    @Column(name = "material_name", nullable = false)
    private String materialName;

    /** 供应商名称快照 */
    @Column(name = "supplier_name")
    private String supplierName;

    /** 操作人 */
    @Column(name = "operator")
    private String operator;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 源出库单号（转包出库生成的出库单） */
    @Column(name = "source_outbound_doc_no", length = 50)
    private String sourceOutboundDocNo;

    /** 目标入库单号（转包入库生成的新入库单） */
    @Column(name = "target_inbound_doc_no", length = 50)
    private String targetInboundDocNo;

    /** 转包类型：拆包（向下转包）/ 合包（向上转包） */
    @Column(name = "transfer_type", length = 20)
    private String transferType;
}
