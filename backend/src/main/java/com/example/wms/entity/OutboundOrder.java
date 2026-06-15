package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "outbound_order")
public class OutboundOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_no", nullable = false, unique = true, length = 50)
    private String docNo;

    @Column(nullable = false, length = 100)
    private String supplier;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount = 0;

    @Column(name = "planned_total_qty", nullable = false)
    private Integer plannedTotalQty = 0;

    @Column(name = "actual_total_qty", nullable = false)
    private Integer actualTotalQty = 0;

    @Column(length = 255)
    private String remark;
}
