package com.example.wms.service.impl;

import com.example.wms.dto.alert.AlertThresholdDTO;
import com.example.wms.entity.AlertThreshold;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InventoryStock;
import com.example.wms.repository.AlertThresholdRepository;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.service.AlertThresholdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高低储预警阈值管理 Service 实现
 */
@Service
public class AlertThresholdServiceImpl implements AlertThresholdService {

    private final AlertThresholdRepository thresholdRepo;
    private final InventoryStockRepository stockRepo;
    private final InboundKanbanLabelRepository kanbanLabelRepo;

    public AlertThresholdServiceImpl(AlertThresholdRepository thresholdRepo,
                                     InventoryStockRepository stockRepo,
                                     InboundKanbanLabelRepository kanbanLabelRepo) {
        this.thresholdRepo = thresholdRepo;
        this.stockRepo = stockRepo;
        this.kanbanLabelRepo = kanbanLabelRepo;
    }

    @Override
    public List<AlertThresholdDTO> listAll() {
        // 读取所有库存（按物料+供应商分组，跨库区求和）
        List<InventoryStock> allStocks = stockRepo.findAll();
        Map<String, Integer> stockMap = new LinkedHashMap<>();
        Map<String, String> nameMap = new LinkedHashMap<>();
        for (InventoryStock s : allStocks) {
            String key = s.getSupplier() + "::" + s.getMaterialCode();
            stockMap.merge(key, s.getOnHandQty() != null ? s.getOnHandQty() : 0, Integer::sum);
            nameMap.putIfAbsent(key, s.getMaterialName());
        }

        // 剔除已封存的库存数量（看板标签 sealed=true 的数量不计入当前库存）
        List<InboundKanbanLabel> sealedLabels = kanbanLabelRepo.findBySealedTrue();
        for (InboundKanbanLabel label : sealedLabels) {
            if (label.getLabelQty() == null || label.getLabelQty() <= 0) continue;
            String key = label.getSupplierName() + "::" + label.getMaterialCode();
            Integer current = stockMap.getOrDefault(key, 0);
            int deducted = Math.max(0, current - label.getLabelQty());
            stockMap.put(key, deducted);
        }

        // 读取已配置的阈值
        List<AlertThreshold> thresholds = thresholdRepo.findAll();
        Map<String, AlertThreshold> thresholdMap = thresholds.stream()
                .collect(Collectors.toMap(
                        t -> t.getSupplier() + "::" + t.getMaterialCode(),
                        t -> t,
                        (a, b) -> a
                ));

        // 合并输出：有库存或有阈值的物料都展示
        Set<String> allKeys = new LinkedHashSet<>(stockMap.keySet());
        thresholdMap.keySet().forEach(allKeys::add);

        List<AlertThresholdDTO> result = new ArrayList<>();
        for (String key : allKeys) {
            String[] parts = key.split("::", 2);
            String supplier = parts[0];
            String materialCode = parts[1];
            String materialName = nameMap.getOrDefault(key, "");
            Integer currentStock = stockMap.getOrDefault(key, 0);
            AlertThreshold threshold = thresholdMap.get(key);

            AlertThresholdDTO dto = new AlertThresholdDTO();
            dto.setId(threshold != null ? threshold.getId() : null);
            dto.setMaterialCode(materialCode);
            dto.setMaterialName(materialName);
            dto.setSupplier(supplier);
            dto.setCurrentStock(currentStock);
            dto.setLowStockQty(threshold != null ? threshold.getLowStockQty() : 0);
            dto.setHighStockQty(threshold != null ? threshold.getHighStockQty() : 0);
            dto.setUpdatedAt(threshold != null ? threshold.getUpdatedAt() : null);

            // 计算预警状态
            int low = dto.getLowStockQty() != null ? dto.getLowStockQty() : 0;
            int high = dto.getHighStockQty() != null ? dto.getHighStockQty() : 0;
            if (low > 0 && currentStock < low) {
                dto.setAlertStatus("low");
            } else if (high > 0 && currentStock > high) {
                dto.setAlertStatus("high");
            } else if (low > 0 || high > 0) {
                dto.setAlertStatus("normal");
            } else {
                dto.setAlertStatus("-");
            }

            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional
    public void batchSave(List<AlertThresholdDTO> list, String operator) {
        String operatorName = (operator != null && !operator.trim().isEmpty()) ? operator : "system";
        LocalDateTime now = LocalDateTime.now();

        List<AlertThreshold> toSave = new ArrayList<>();
        for (AlertThresholdDTO dto : list) {
            if (dto.getMaterialCode() == null || dto.getMaterialCode().trim().isEmpty()) continue;
            AlertThreshold entity = thresholdRepo
                    .findByMaterialCodeAndSupplier(dto.getMaterialCode().trim(), dto.getSupplier().trim())
                    .orElseGet(() -> {
                        AlertThreshold t = new AlertThreshold();
                        t.setMaterialCode(dto.getMaterialCode().trim());
                        t.setSupplier(dto.getSupplier().trim());
                        t.setCreatedBy(operatorName);
                        t.setCreatedAt(now);
                        return t;
                    });
            entity.setMaterialName(dto.getMaterialName() != null ? dto.getMaterialName().trim() : "");
            entity.setLowStockQty(dto.getLowStockQty() != null ? dto.getLowStockQty() : 0);
            entity.setHighStockQty(dto.getHighStockQty() != null ? dto.getHighStockQty() : 0);
            entity.setUpdatedBy(operatorName);
            entity.setUpdatedAt(now);
            toSave.add(entity);
        }
        thresholdRepo.saveAll(toSave);
    }
}
