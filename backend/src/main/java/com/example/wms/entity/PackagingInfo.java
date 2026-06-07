package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "packaging_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_packaging_info_material_supplier", columnNames = {"material_no", "supplier_code"}))
public class PackagingInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_no", nullable = false, length = 50)
    private String materialNo;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "package_model", length = 50)
    private String packageModel;

    @Column(name = "package_capacity")
    private Integer packageCapacity;
}
