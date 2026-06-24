package com.example.wms.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WarehouseAreaDTO {

    private Long id;
    private String areaCode;
    private String areaName;
    private Integer sortOrder;
    private String description;
}
