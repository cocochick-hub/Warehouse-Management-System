package com.example.wms.service;

import com.example.wms.dto.inbound.InboundOrderCreateRequest;
import com.example.wms.dto.inbound.InboundOrderDetailResponse;
import com.example.wms.dto.inbound.InboundOrderPageResponse;
import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.inbound.InboundReceiveRequest;
import com.example.wms.dto.inbound.InboundScanReceiveRequest;

import java.util.List;

public interface InboundOrderService {

    InboundOrderPageResponse listOrders(String docNo, String supplier, String status, Integer page, Integer size);

    InboundOrderDetailResponse getOrderDetail(Long id);

    InboundOrderDetailResponse createOrder(InboundOrderCreateRequest request, String operator);

    InboundOrderDetailResponse receiveOrder(Long id, InboundReceiveRequest request, String operator);

    List<InboundKanbanLabelDTO> generateKanbanLabels(Long id, String operator);

    List<InboundKanbanLabelDTO> listKanbanLabels(Long id);

    InboundKanbanLabelDTO getScanLabel(String kanbanNoOrPayload);

    InboundOrderDetailResponse receiveByScan(InboundScanReceiveRequest request, String operator);

    InboundOrderPageResponse listHistory(String docNo, String supplier, String materialCode, String transferStatus, String warehouseArea, Integer page, Integer size);
}
