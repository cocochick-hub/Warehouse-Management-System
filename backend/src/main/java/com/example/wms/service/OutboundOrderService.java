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

import org.springframework.data.domain.Page;

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
}
