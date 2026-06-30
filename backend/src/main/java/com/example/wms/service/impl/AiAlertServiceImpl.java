package com.example.wms.service.impl;

import com.example.wms.entity.AiAlert;
import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.repository.AiAlertRepository;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.repository.OutboundHistoryRepository;
import com.example.wms.service.AiAlertService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 预警分析服务实现
 * 核心职责：计算缺货风险 & 呆滞报废风险
 */
@Service
public class AiAlertServiceImpl implements AiAlertService {

    private static final Logger log = LoggerFactory.getLogger(AiAlertServiceImpl.class);

    private final InventoryStockRepository stockRepo;
    private final OutboundHistoryRepository historyRepo;
    private final AiAlertRepository alertRepo;
    private final ObjectMapper objectMapper;

    /** 日均消耗计算窗口（天） */
    private static final int CONSUMPTION_WINDOW_DAYS = 30;

    public AiAlertServiceImpl(InventoryStockRepository stockRepo,
                              OutboundHistoryRepository historyRepo,
                              AiAlertRepository alertRepo,
                              ObjectMapper objectMapper) {
        this.stockRepo = stockRepo;
        this.historyRepo = historyRepo;
        this.alertRepo = alertRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<AiAlert> runFullAnalysis() {
        log.info("===== AI 预警全量分析开始 =====");
        long start = System.currentTimeMillis();

        // 清除旧预警
        alertRepo.deleteAllAlerts();
        log.info("已清除旧预警记录");

        List<AiAlert> allAlerts = new ArrayList<>();

        // 1. 缺货预测
        List<AiAlert> shortageAlerts = analyzeShortage();
        allAlerts.addAll(shortageAlerts);
        log.info("缺货预测完成，共 {} 条预警", shortageAlerts.size());

        // 2. 呆滞报废预警
        List<AiAlert> deadStockAlerts = analyzeDeadStock();
        allAlerts.addAll(deadStockAlerts);
        log.info("呆滞预警完成，共 {} 条预警", deadStockAlerts.size());

        // 批量保存
        alertRepo.saveAll(allAlerts);

        long elapsed = System.currentTimeMillis() - start;
        log.info("===== AI 预警全量分析完成，总耗时 {}ms =====", elapsed);

        return allAlerts;
    }

    /**
     * 缺货预测分析
     * 逻辑：基于近30天出库历史计算日均消耗 → 预估可支撑天数 → 分级预警
     */
    private List<AiAlert> analyzeShortage() {
        List<InventoryStock> stocks = stockRepo.findAll();
        if (stocks.isEmpty()) return Collections.emptyList();

        // 获取近30天所有出库记录，按物料汇总消耗量
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(CONSUMPTION_WINDOW_DAYS);
        List<OutboundHistory> recentHistory = historyRepo.findByCreatedAtAfter(thirtyDaysAgo);

        Map<String, Integer> consumptionMap = recentHistory.stream()
                .filter(h -> h.getMaterialCode() != null)
                .collect(Collectors.groupingBy(
                        OutboundHistory::getMaterialCode,
                        Collectors.summingInt(h -> h.getIssueQty() != null ? h.getIssueQty() : 0)
                ));

        List<AiAlert> alerts = new ArrayList<>();

        for (InventoryStock stock : stocks) {
            // 只分析有库存的物料
            if (stock.getOnHandQty() == null || stock.getOnHandQty() <= 0) continue;

            String code = stock.getMaterialCode();
            Integer totalConsumed = consumptionMap.getOrDefault(code, 0);

            // 日均消耗
            BigDecimal dailyConsumption = BigDecimal.valueOf(totalConsumed)
                    .divide(BigDecimal.valueOf(CONSUMPTION_WINDOW_DAYS), 2, RoundingMode.HALF_UP);

            // 预估可支撑天数
            int estimatedDays;
            String riskLevel;
            String suggestion;

            if (dailyConsumption.compareTo(BigDecimal.ZERO) == 0) {
                // 无出库记录，无法预测缺货 → 不算缺货风险
                continue;
            }

            estimatedDays = BigDecimal.valueOf(stock.getOnHandQty())
                    .divide(dailyConsumption, 0, RoundingMode.DOWN)
                    .intValue();

            // 风险等级判定
            if (estimatedDays < 7) {
                riskLevel = "HIGH";
                suggestion = String.format("【紧急】%s 当前库存 %d，日均消耗 %.1f，预计仅能支撑 %d 天，建议立即安排补货。",
                        stock.getMaterialName(), stock.getOnHandQty(), dailyConsumption, estimatedDays);
            } else if (estimatedDays <= 14) {
                riskLevel = "MEDIUM";
                suggestion = String.format("【关注】%s 当前库存 %d，日均消耗 %.1f，预计可支撑 %d 天，建议制定补货计划。",
                        stock.getMaterialName(), stock.getOnHandQty(), dailyConsumption, estimatedDays);
            } else {
                riskLevel = "LOW";
                suggestion = String.format("【正常】%s 当前库存 %d，日均消耗 %.1f，预计可支撑 %d 天，库存充足。",
                        stock.getMaterialName(), stock.getOnHandQty(), dailyConsumption, estimatedDays);
            }

            AiAlert alert = new AiAlert();
            alert.setMaterialCode(code);
            alert.setMaterialName(stock.getMaterialName());
            alert.setAlertType("SHORTAGE");
            alert.setRiskLevel(riskLevel);
            alert.setCurrentStock(stock.getOnHandQty());
            alert.setDailyConsumption(dailyConsumption);
            alert.setEstimatedDays(estimatedDays);
            alert.setIdleDays(null);
            alert.setSuggestion(suggestion);
            alert.setAnalysisJson(buildShortageJson(stock, dailyConsumption, estimatedDays, totalConsumed));

            alerts.add(alert);
        }

        // 按风险等级排序：HIGH → MEDIUM → LOW
        alerts.sort(Comparator.comparingInt(a -> {
            switch (a.getRiskLevel()) {
                case "HIGH": return 0;
                case "MEDIUM": return 1;
                default: return 2;
            }
        }));

        return alerts;
    }

    /**
     * 呆滞报废预警分析
     * 逻辑：计算每种物料距上次出库的天数 → 分级预警
     */
    private List<AiAlert> analyzeDeadStock() {
        List<InventoryStock> stocks = stockRepo.findAll();
        if (stocks.isEmpty()) return Collections.emptyList();

        // 获取全部出库记录，按物料找最后出库日期
        List<OutboundHistory> allHistory = historyRepo.findAll();
        Map<String, LocalDateTime> lastOutboundMap = allHistory.stream()
                .filter(h -> h.getMaterialCode() != null && h.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        OutboundHistory::getMaterialCode,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(OutboundHistory::getCreatedAt)),
                                opt -> opt.map(OutboundHistory::getCreatedAt).orElse(null)
                        )
                ));

        LocalDateTime now = LocalDateTime.now();
        List<AiAlert> alerts = new ArrayList<>();

        for (InventoryStock stock : stocks) {
            if (stock.getOnHandQty() == null || stock.getOnHandQty() <= 0) continue;

            String code = stock.getMaterialCode();
            LocalDateTime lastOutbound = lastOutboundMap.get(code);

            long idleDays;
            if (lastOutbound != null) {
                idleDays = ChronoUnit.DAYS.between(lastOutbound, now);
            } else {
                // 从未出库，以最后入库时间或创建时间为准
                LocalDateTime ref = stock.getLastInboundAt() != null ? stock.getLastInboundAt() : stock.getCreatedAt();
                if (ref == null) continue;
                idleDays = ChronoUnit.DAYS.between(ref, now);
            }

            // 不到30天不算呆滞
            if (idleDays < 30) continue;

            String riskLevel;
            String suggestion;

            if (idleDays > 90) {
                riskLevel = "HIGH";
                suggestion = String.format("【严重】%s 已呆滞 %d 天无出库记录，建议立即评估是否报废处理或降价出清。",
                        stock.getMaterialName(), idleDays);
            } else if (idleDays >= 60) {
                riskLevel = "MEDIUM";
                suggestion = String.format("【关注】%s 已 %d 天无出库记录，存在呆滞风险，建议关注并制定处置方案。",
                        stock.getMaterialName(), idleDays);
            } else {
                riskLevel = "LOW";
                suggestion = String.format("【提示】%s 已 %d 天无出库记录，建议持续观察消耗情况。",
                        stock.getMaterialName(), idleDays);
            }

            AiAlert alert = new AiAlert();
            alert.setMaterialCode(code);
            alert.setMaterialName(stock.getMaterialName());
            alert.setAlertType("DEAD_STOCK");
            alert.setRiskLevel(riskLevel);
            alert.setCurrentStock(stock.getOnHandQty());
            alert.setDailyConsumption(null);
            alert.setEstimatedDays(null);
            alert.setIdleDays((int) idleDays);
            alert.setSuggestion(suggestion);
            alert.setAnalysisJson(buildDeadStockJson(stock, idleDays, lastOutbound));

            alerts.add(alert);
        }

        // 按风险等级排序
        alerts.sort(Comparator.comparingInt(a -> {
            switch (a.getRiskLevel()) {
                case "HIGH": return 0;
                case "MEDIUM": return 1;
                default: return 2;
            }
        }));

        return alerts;
    }

    private String buildShortageJson(InventoryStock stock, BigDecimal dailyConsumption,
                                      int estimatedDays, int totalConsumed) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("materialCode", stock.getMaterialCode());
            data.put("materialName", stock.getMaterialName());
            data.put("currentStock", stock.getOnHandQty());
            data.put("dailyConsumption", dailyConsumption);
            data.put("estimatedDays", estimatedDays);
            data.put("totalConsumed30d", totalConsumed);
            data.put("analysisWindow", CONSUMPTION_WINDOW_DAYS + "天");
            data.put("warehouseArea", stock.getWarehouseArea());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String buildDeadStockJson(InventoryStock stock, long idleDays, LocalDateTime lastOutbound) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("materialCode", stock.getMaterialCode());
            data.put("materialName", stock.getMaterialName());
            data.put("currentStock", stock.getOnHandQty());
            data.put("idleDays", idleDays);
            data.put("lastOutboundAt", lastOutbound != null ? lastOutbound.toString() : null);
            data.put("lastInboundAt", stock.getLastInboundAt() != null ? stock.getLastInboundAt().toString() : null);
            data.put("warehouseArea", stock.getWarehouseArea());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
