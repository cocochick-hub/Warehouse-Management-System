package com.example.wms.service.impl;

import com.example.wms.dto.DashboardDTO;
import com.example.wms.dto.PendingTaskDTO;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.OutboundOrder;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;

    public DashboardServiceImpl(InboundOrderRepository inboundOrderRepository,
                                OutboundOrderRepository outboundOrderRepository) {
        this.inboundOrderRepository = inboundOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
    }

    @Override
    public DashboardDTO getDashboardData() {
        List<InboundOrder> inboundOrders = inboundOrderRepository.findByStatusNotOrderByCreatedAtDesc("已完成");
        List<OutboundOrder> outboundOrders = outboundOrderRepository.findByStatusNotOrderByCreatedAtDesc("已完成");

        int pendingInbound = 0;
        int pendingOutbound = 0;
        List<PendingTaskDTO> pendingTasks = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (InboundOrder order : inboundOrders) {
            pendingInbound++;
            String color;
            switch (order.getStatus()) {
                case "未入库":
                    color = "info";
                    break;
                case "部分完成":
                    color = "warning";
                    break;
                default:
                    color = "info";
            }
            String date = order.getCreatedAt() != null
                    ? order.getCreatedAt().toLocalDate().format(fmt)
                    : LocalDate.now().format(fmt);
            pendingTasks.add(new PendingTaskDTO("入库", order.getDocNo(), order.getSupplier(), order.getStatus(), color, date));
        }

        for (OutboundOrder order : outboundOrders) {
            pendingOutbound++;
            String date = order.getCreatedAt() != null
                    ? order.getCreatedAt().toLocalDate().format(fmt)
                    : LocalDate.now().format(fmt);
            pendingTasks.add(new PendingTaskDTO("出库", order.getDocNo(), order.getSupplier(), order.getStatus(), "primary", date));
        }

        return new DashboardDTO(pendingInbound, pendingOutbound, 2, 128, pendingTasks);
    }
}
