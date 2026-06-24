package com.example.wms.service;

import com.example.wms.dto.demand.DemandBatchDTO;
import com.example.wms.dto.demand.DemandCreateRequest;
import com.example.wms.dto.demand.DemandDetailDTO;
import org.springframework.data.domain.Page;

/** 物料需求管理 Service */
public interface DemandService {

    /** 手工创建需求 */
    DemandBatchDTO createDemand(DemandCreateRequest request, String operator);

    /** 分页查询需求明细列表 */
    Page<DemandDetailDTO> listDemands(Integer page, Integer size, String materialCode, String materialName, String supplier, String status);

    /** 查询批次详情 */
    DemandBatchDTO getBatch(String batchNo);
}
