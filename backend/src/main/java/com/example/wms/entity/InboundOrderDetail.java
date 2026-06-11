package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(
        name = "inbound_order_detail",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inbound_order_detail_order_supplier_material",
                        columnNames = {"inbound_order_id", "supplier_code", "material_code"}
                )
        }
)
public class InboundOrderDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_order_id", nullable = false)
    private Long inboundOrderId;

    @Column(name = "doc_no", nullable = false, length = 50)
    private String docNo;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "package_model", length = 50)
    private String packageModel;

    @Column(name = "packaging_capacity")
    private Integer packagingCapacity;

    @Column(name = "planned_qty", nullable = false)
    private Integer plannedQty;

    @Column(name = "actual_qty", nullable = false)
    private Integer actualQty;

    @Column(name = "package_count", nullable = false)
    private Integer packageCount;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    @Column(name = "transfer_status", length = 20)
    private String transferStatus;

    @Column(length = 255)
    private String remark;
}
