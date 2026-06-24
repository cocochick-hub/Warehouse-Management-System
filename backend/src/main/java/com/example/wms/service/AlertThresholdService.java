package com.example.wms.service;

import com.example.wms.dto.alert.AlertThresholdDTO;

import java.util.List;

/** 高低储预警阈值管理 Service */
public interface AlertThresholdService {

    /** 获取所有物料的预警阈值列表（含库存状态） */
    List<AlertThresholdDTO> listAll();

    /** 批量保存/更新阈值（admin 编辑） */
    void batchSave(List<AlertThresholdDTO> list, String operator);
}
