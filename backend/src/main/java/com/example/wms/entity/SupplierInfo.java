package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "supplier_info")
public class SupplierInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_code", nullable = false, unique = true, length = 50)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, unique = true, length = 100)
    private String supplierName;

    @Column(name = "contact", length = 50)
    private String contact;

    @Column(name = "phone", length = 30)
    private String phone;
}
