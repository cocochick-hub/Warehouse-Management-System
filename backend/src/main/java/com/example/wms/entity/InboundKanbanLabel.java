package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "inbound_kanban_label",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inbound_kanban_label_no", columnNames = {"kanban_no"})
        }
)
public class InboundKanbanLabel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_order_id", nullable = false)
    private Long inboundOrderId;

    @Column(name = "inbound_order_detail_id", nullable = false)
    private Long inboundOrderDetailId;

    @Column(name = "doc_no", nullable = false, length = 50)
    private String docNo;

    @Column(name = "kanban_no", nullable = false, length = 100)
    private String kanbanNo;

    @Column(name = "qr_payload", nullable = false, length = 255)
    private String qrPayload;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "package_model", length = 50)
    private String packageModel;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    @Column(name = "label_qty", nullable = false)
    private Integer labelQty;

    @Column(name = "package_seq", nullable = false)
    private Integer packageSeq;

    @Column(name = "package_total", nullable = false)
    private Integer packageTotal;

    @Column(name = "transfer_status", length = 20)
    private String transferStatus;

    @Column(name = "label_status", nullable = false, length = 20)
    private String labelStatus;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "received_by", length = 50)
    private String receivedBy;
}
