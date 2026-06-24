package com.example.wms.config;

import com.example.wms.service.AiAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 预警定时调度器
 * 每小时执行一次全量分析，确保预警数据保持新鲜
 */
@Component
public class AiScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiScheduler.class);

    private final AiAlertService aiAlertService;

    public AiScheduler(AiAlertService aiAlertService) {
        this.aiAlertService = aiAlertService;
    }

    /**
     * 每小时执行一次全量分析
     * 首次启动后延迟 30 秒执行（等待 JPA 初始化完成）
     */
    @Scheduled(initialDelay = 30_000, fixedRate = 3_600_000)
    public void runAnalysis() {
        try {
            aiAlertService.runFullAnalysis();
        } catch (Exception e) {
            log.error("AI 预警定时分析执行失败", e);
        }
    }
}
