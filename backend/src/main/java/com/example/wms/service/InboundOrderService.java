package com.example.wms.service;

import com.example.wms.dto.inbound.InboundOrderCreateRequest;
import com.example.wms.dto.inbound.InboundOrderDetailResponse;
import com.example.wms.dto.inbound.InboundOrderPageResponse;
import com.example.wms.dto.inbound.InboundReceiveRequest;

public interface InboundOrderService {

    InboundOrderPageResponse listOrders(String docNo, String supplier, String status, Integer page, Integer size);

    InboundOrderDetailResponse getOrderDetail(Long id);

    InboundOrderDetailResponse createOrder(InboundOrderCreateRequest request, String operator);

    InboundOrderDetailResponse receiveOrder(Long id, InboundReceiveRequest request, String operator);
}
