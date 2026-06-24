package com.example.wms.service;

import com.example.wms.dto.seal.SealBatchResultDTO;
import com.example.wms.dto.seal.SealLabelDTO;
import com.example.wms.dto.seal.SealLabelRequest;

import java.util.List;

public interface SealService {

    /** 根据看板号查询封存信息 */
    SealLabelDTO getLabelByKanbanNo(String kanbanNo);

    /** 查询所有已封存的看板 */
    List<SealLabelDTO> listSealedLabels(String materialCode, String supplierName);

    /** 单个封存/解封操作（扫码） */
    SealLabelDTO toggleSealSingle(String kanbanNo, String action, String operator);

    /** 批量封存/解封操作 */
    SealBatchResultDTO toggleSealBatch(SealLabelRequest request, String operator);
}
