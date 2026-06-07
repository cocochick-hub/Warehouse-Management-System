package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "material_info")
public class MaterialInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_no", nullable = false, unique = true, length = 50)
    private String materialNo;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "material_type", length = 50)
    private String materialType;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;
}
