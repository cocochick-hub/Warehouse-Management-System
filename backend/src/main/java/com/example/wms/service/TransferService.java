package com.example.wms.service;

import com.example.wms.dto.transfer.TransferHistoryPageResponse;
import com.example.wms.dto.transfer.TransferKanbanPageResponse;
import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.dto.transfer.TransferResultDTO;

public interface TransferService {

    /**
     * 执行转包操作：从源看板转移指定数量到新看板
     *
     * 完整流程：
     * 1. 创建源看板的出库单（OutboundOrder + OutboundOrderDetail）
     * 2. 扣减源物料库存（InventoryStock.onHandQty）
     * 3. 创建目标看板的新入库单（InboundOrder + InboundOrderDetail）
     * 4. 增加目标物料库存（InventoryStock.onHandQty）
     * 5. 创建目标看板标签（InboundKanbanLabel）
     * 6. 更新源看板状态（转包/已转包）
     * 7. 记录转包历史（PackageTransfer）
     *
     * @param request 转包请求
     * @param operator 操作人
     * @return 转包结果
     */
    TransferResultDTO executeTransfer(TransferRequest request, String operator);

    /**
     * 分页查询可转包的看板列表
     * 条件：状态为"已入库"且未封存
     *
     * @param materialCode 物料编码（可选，模糊匹配）
     * @param supplierName 供应商名称（可选，模糊匹配）
     * @param page 页码
     * @param size 每页大小
     * @return 可转包看板分页列表
     */
    TransferKanbanPageResponse listAvailableKanbans(String materialCode, String supplierName, Integer page, Integer size);

    /**
     * 分页查询转包历史记录
     *
     * @param sourceKanbanNo 源看板号（可选，模糊匹配）
     * @param targetKanbanNo 目标看板号（可选，模糊匹配）
     * @param page 页码
     * @param size 每页大小
     * @return 转包历史分页列表
     */
    TransferHistoryPageResponse listTransferHistory(String sourceKanbanNo, String targetKanbanNo, Integer page, Integer size);
}
