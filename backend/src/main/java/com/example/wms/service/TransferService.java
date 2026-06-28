package com.example.wms.service;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.dto.transfer.TransferResultDTO;

public interface TransferService {

    /** 执行转包操作：从源看板转移指定数量到新看板 */
    TransferResultDTO executeTransfer(TransferRequest request, String operator);
}
