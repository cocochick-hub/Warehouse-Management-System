package com.example.wms.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转包历史分页响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferHistoryPageResponse {

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页大小 */
    private int size;

    /** 记录列表 */
    private List<TransferHistoryDTO> records;
}
