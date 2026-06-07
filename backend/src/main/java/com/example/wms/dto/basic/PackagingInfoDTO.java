package com.example.wms.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PackagingInfoDTO {

    private Long id;
    private String materialNo;
    private String supplierCode;
    private String packageModel;
    private Integer packageCapacity;
}
