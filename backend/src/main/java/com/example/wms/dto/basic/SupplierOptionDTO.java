package com.example.wms.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupplierOptionDTO {

    private Long id;
    private String supplierCode;
    private String supplierName;
    private String contact;
    private String phone;
}
