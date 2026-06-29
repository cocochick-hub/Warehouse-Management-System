package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "outbound_history")
public class OutboundHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_order_id", nullable = false)
    private Long outboundOrderId;

    @Column(name = "outbound_detail_id", nullable = false)
    private Long outboundDetailId;

    @Column(name = "doc_no", nullable = false, length = 50)
    private String docNo;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "issue_qty", nullable = false)
    private Integer issueQty;

    @Column(name = "source_inbound_doc", length = 50)
    private String sourceInboundDoc;

    @Column(name = "source_detail_id")
    private Long sourceDetailId;

    @Column(name = "kanban_label_id")
    private Long kanbanLabelId;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea = "默认库区";

    @Column(name = "issued_by", length = 50)
    private String issuedBy;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
