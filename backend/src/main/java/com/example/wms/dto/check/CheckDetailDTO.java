package com.example.wms.dto.check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckDetailDTO {

    private Long id;
    private Long taskId;
    private String taskNo;
    private String materialCode;
    private String materialName;
    private String supplier;
    private String warehouseArea;
    private Integer systemQty;
    private Integer actualQty;
    private Integer diffQty;
    private String status;
    private String checkedBy;
    private LocalDateTime checkedAt;
    private LocalDateTime createdAt;
}