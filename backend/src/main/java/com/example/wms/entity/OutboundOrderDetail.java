package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(
        name = "outbound_order_detail",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_outbound_order_detail_order_supplier_material",
                        columnNames = {"outbound_order_id", "supplier_code", "material_code"}
                )
        }
)
public class OutboundOrderDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_order_id", nullable = false)
    private Long outboundOrderId;

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

    @Column(name = "planned_qty", nullable = false)
    private Integer plannedQty;

    @Column(name = "actual_qty", nullable = false)
    private Integer actualQty = 0;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea = "默认库区";

    @Column(length = 255)
    private String remark;
}
