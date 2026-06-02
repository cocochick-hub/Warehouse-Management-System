package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "inbound_order")
public class InboundOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_no", nullable = false, unique = true, length = 50)
    private String docNo;

    @Column(nullable = false, length = 100)
    private String supplier;

    @Column(nullable = false, length = 20)
    private String status;
}
