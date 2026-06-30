package com.example.wms.service;

import com.example.wms.entity.AiAlert;

import java.util.List;

/**
 * AI 预警分析服务接口
 * 定义缺货预测和呆滞报废预警的核心分析方法
 */
public interface AiAlertService {

    /**
     * 执行全量分析：清除旧结果 → 缺货分析 → 呆滞分析
     *
     * @return 全部预警记录列表（按风险等级排序）
     */
    List<AiAlert> runFullAnalysis();
}
