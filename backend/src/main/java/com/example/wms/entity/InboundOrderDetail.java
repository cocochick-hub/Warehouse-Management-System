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
                        name = "uk_inbound_order_detail_order_material",
                        columnNames = {"inbound_order_id", "material_code"}
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

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "packaging_capacity")
    private Integer packagingCapacity;

    @Column(name = "planned_qty", nullable = false)
    private Integer plannedQty;

    @Column(name = "actual_qty", nullable = false)
    private Integer actualQty;

    @Column(length = 255)
    private String remark;
}
