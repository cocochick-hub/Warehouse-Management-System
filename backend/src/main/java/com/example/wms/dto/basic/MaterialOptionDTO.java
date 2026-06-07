package com.example.wms.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MaterialOptionDTO {

    private Long id;
    private String materialNo;
    private String materialName;
    private String materialType;
    private String unit;
    private String supplierCode;
    private String supplierName;
    private Integer packageCapacity;
    private String packageModel;
}
