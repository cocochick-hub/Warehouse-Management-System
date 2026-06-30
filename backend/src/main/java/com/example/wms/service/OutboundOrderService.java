package com.example.wms.service;

import com.example.wms.dto.outbound.OutboundOrderCreateRequest;
import com.example.wms.dto.outbound.OutboundOrderDetailResponse;
import com.example.wms.dto.outbound.OutboundOrderPageResponse;
import com.example.wms.dto.outbound.OutboundHistoryDTO;
import com.example.wms.dto.outbound.OutboundIssueRequest;
import com.example.wms.dto.outbound.OutboundOrderlessRequest;
import com.example.wms.dto.outbound.OutboundScanIssueRequest;
import com.example.wms.dto.outbound.OutboundReturnLabelResponse;
import com.example.wms.dto.outbound.OutboundReturnRequest;
import com.example.wms.dto.outbound.OutboundReturnResponse;
import com.example.wms.dto.outbound.OutboundScanLabelResponse;
import com.example.wms.dto.outbound.OutboundKanbanLabelDTO;
import com.example.wms.dto.outbound.OutboundIssueByLabelRequest;
import com.example.wms.dto.outbound.OutboundIssuedLabelDTO;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface OutboundOrderService {

    OutboundOrderPageResponse listOrders(String docNo, String supplier, String status, Integer page, Integer size);

    OutboundOrderDetailResponse getOrderDetail(Long id);

    OutboundOrderDetailResponse createOrder(OutboundOrderCreateRequest request, String operator);

    OutboundOrderDetailResponse issueOrder(Long id, OutboundIssueRequest request, String operator);

    Page<OutboundHistoryDTO> listHistory(String docNo, Integer page, Integer size);

    OutboundScanLabelResponse getOutboundScanLabel(String kanbanNo);

    OutboundOrderDetailResponse issueByScan(OutboundScanIssueRequest request, String operator);

    /**
     * 扫码查询退库信息 — 根据看板号查找关联的出库记录
     */
    OutboundReturnLabelResponse getReturnLabel(String kanbanNo);

    /**
     * 扫码执行退库 — 库存回增 + 出库历史标记已退库
     */
    OutboundReturnResponse returnByScan(OutboundReturnRequest request, String operator);

    OutboundOrderDetailResponse issueWithoutOrder(OutboundOrderlessRequest request, String operator);

    /**
     * 获取出库单可用的看板标签列表
     * 按物料编码+供应商名称匹配出库单明细，返回已入库、未封存、未出库的看板
     */
    List<OutboundKanbanLabelDTO> getAvailableKanbanLabels(Long orderId);

    /**
     * 按看板标签出库（多选模式）
     * 自动根据物料编码+供应商名称匹配出库单明细行，校验数量不超过计划
     */
    OutboundOrderDetailResponse issueByLabels(Long orderId, List<Long> labelIds, Map<Long, Integer> labelIssueQtys, String operator);

    /**
     * 获取出库单已出库的看板标签列表（供退库选择）
     * 返回该出库单下已出库但未退库的看板
     */
    List<OutboundIssuedLabelDTO> getIssuedLabels(Long orderId);

    /**
     * 批量退库（多选看板模式）
     * 库存回增 + 出库历史标记已退库 + 看板重置 + 更新出库明细实发数量
     */
    OutboundOrderDetailResponse returnByLabels(Long orderId, List<Long> labelIds, String operator);
}
