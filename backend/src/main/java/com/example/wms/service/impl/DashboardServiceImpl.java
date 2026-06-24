package com.example.wms.service.impl;

import com.example.wms.dto.DashboardDTO;
import com.example.wms.dto.PendingTaskDTO;
import com.example.wms.entity.AlertThreshold;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.OutboundOrder;
import com.example.wms.repository.*;
import com.example.wms.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final MaterialInfoRepository materialInfoRepository;
    private final AlertThresholdRepository alertThresholdRepository;

    public DashboardServiceImpl(InboundOrderRepository inboundOrderRepository,
                                OutboundOrderRepository outboundOrderRepository,
                                InventoryStockRepository inventoryStockRepository,
                                MaterialInfoRepository materialInfoRepository,
                                AlertThresholdRepository alertThresholdRepository) {
        this.inboundOrderRepository = inboundOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.materialInfoRepository = materialInfoRepository;
        this.alertThresholdRepository = alertThresholdRepository;
    }

    @Override
    public DashboardDTO getDashboardData() {
        int pendingInbound = inboundOrderRepository.countByStatusNot("已完成");
        int pendingOutbound = outboundOrderRepository.countByStatusNot("已完成");
        int lowStockAlert = inventoryStockRepository.countByOnHandQty(0);
        int totalMaterials = (int) materialInfoRepository.count();

        List<InboundOrder> inboundOrders = inboundOrderRepository.findByStatusNotOrderByCreatedAtDesc("已完成");
        List<OutboundOrder> outboundOrders = outboundOrderRepository.findByStatusNotOrderByCreatedAtDesc("已完成");

        List<PendingTaskDTO> pendingTasks = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (InboundOrder order : inboundOrders) {
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
            String date = order.getCreatedAt() != null
                    ? order.getCreatedAt().toLocalDate().format(fmt)
                    : LocalDate.now().format(fmt);
            pendingTasks.add(new PendingTaskDTO("出库", order.getDocNo(), order.getSupplier(), order.getStatus(), "primary", date));
        }

        // === 计算库存健康度（与 alert_threshold 联动） ===
        int healthPercent = 100;
        int normalCount = 0;
        int lowAlertCount = 0;
        int highAlertCount = 0;

        List<AlertThreshold> thresholds = alertThresholdRepository.findAll();
        // 库存按 (supplier + materialCode) 分组跨库区求和
        List<InventoryStock> allStocks = inventoryStockRepository.findAll();
        Map<String, Integer> stockMap = new HashMap<>();
        for (InventoryStock s : allStocks) {
            String key = s.getSupplier() + "::" + s.getMaterialCode();
            stockMap.merge(key, s.getOnHandQty() != null ? s.getOnHandQty() : 0, Integer::sum);
        }

        for (AlertThreshold t : thresholds) {
            String key = t.getSupplier() + "::" + t.getMaterialCode();
            int currentStock = stockMap.getOrDefault(key, 0);
            int low = t.getLowStockQty() != null ? t.getLowStockQty() : 0;
            int high = t.getHighStockQty() != null ? t.getHighStockQty() : 0;

            if (low > 0 && currentStock < low) {
                lowAlertCount++;
            } else if (high > 0 && currentStock > high) {
                highAlertCount++;
            } else {
                normalCount++;
            }
        }

        int totalCount = normalCount + lowAlertCount + highAlertCount;
        if (totalCount > 0) {
            healthPercent = (int) ((double) normalCount / totalCount * 100);
        }

        DashboardDTO dto = new DashboardDTO(pendingInbound, pendingOutbound, lowStockAlert, totalMaterials, pendingTasks,
                healthPercent, normalCount, lowAlertCount, highAlertCount);
        return dto;
    }
}
