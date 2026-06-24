package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 预警记录表
 * 存储定时分析的缺货预测和呆滞报废预警结果
 */
@Getter
@Setter
@Entity
@Table(name = "ai_alert")
public class AiAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 物料号 */
    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    /** 物料名称 */
    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    /** 预警类型: SHORTAGE(缺货) / DEAD_STOCK(呆滞) */
    @Column(name = "alert_type", nullable = false, length = 20)
    private String alertType;

    /** 风险等级: HIGH / MEDIUM / LOW */
    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    /** 当前库存 */
    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    /** 日均消耗量 */
    @Column(name = "daily_consumption", precision = 10, scale = 2)
    private BigDecimal dailyConsumption;

    /** 预估可支撑天数 (仅缺货类型) */
    @Column(name = "estimated_days")
    private Integer estimatedDays;

    /** 呆滞天数 (仅呆滞类型) */
    @Column(name = "idle_days")
    private Integer idleDays;

    /** AI 建议文案 */
    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    /** 完整分析数据 JSON */
    @Column(name = "analysis_json", columnDefinition = "TEXT")
    private String analysisJson;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
