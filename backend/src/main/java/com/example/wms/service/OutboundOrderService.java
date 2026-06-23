package com.example.wms.service;

import com.example.wms.dto.outbound.OutboundOrderCreateRequest;
import com.example.wms.dto.outbound.OutboundOrderDetailResponse;
import com.example.wms.dto.outbound.OutboundOrderPageResponse;
import com.example.wms.dto.outbound.OutboundHistoryDTO;
import com.example.wms.dto.outbound.OutboundIssueRequest;
import com.example.wms.dto.outbound.OutboundScanIssueRequest;
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
}
