package com.example.wms.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundHistoryDTO {

    private Long id;
    private String docNo;
    private String materialCode;
    private String materialName;
    private String supplierName;
    private int issueQty;
    private String sourceInboundDoc;
    private String warehouseArea;
    private String issuedBy;
    private LocalDateTime createdAt;
    private String status;
}
