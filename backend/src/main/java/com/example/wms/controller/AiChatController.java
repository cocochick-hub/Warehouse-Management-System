package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.entity.AiAlert;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.repository.AiAlertRepository;
import com.example.wms.repository.InboundOrderDetailRepository;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.repository.OutboundHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 对话控制器
 * 职责：
 * 1. POST /api/ai/chat — 透传 DeepSeek API，注入 Function Calling
 * 2. GET /api/ai/alerts/latest — 返回最新预警结果
 * 3. GET /api/ai/data/* — 供 Function Calling 调用的数据接口
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiAlertRepository alertRepo;
    private final InventoryStockRepository stockRepo;
    private final OutboundHistoryRepository outboundHistoryRepo;
    private final InboundOrderDetailRepository inboundDetailRepo;

    public AiChatController(AiAlertRepository alertRepo,
                            InventoryStockRepository stockRepo,
                            OutboundHistoryRepository outboundHistoryRepo,
                            InboundOrderDetailRepository inboundDetailRepo,
                            ObjectMapper objectMapper) {
        this.alertRepo = alertRepo;
        this.stockRepo = stockRepo;
        this.outboundHistoryRepo = outboundHistoryRepo;
        this.inboundDetailRepo = inboundDetailRepo;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 启动时诊断 DeepSeek 配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void diagnoseDeepSeekConfig() {
        String maskedKey = (apiKey != null && apiKey.length() > 8)
                ? apiKey.substring(0, 8) + "****"
                : (apiKey == null ? "NULL" : "EMPTY");
        log.info("========== DeepSeek 配置诊断 ==========");
        log.info("base-url: {}", baseUrl);
        log.info("model:    {}", model);
        log.info("api-key:  {}", maskedKey);
        if (apiKey == null || apiKey.isEmpty() || "placeholder".equals(apiKey)) {
            log.error("!!! DeepSeek API Key 无效！");
            log.error("!!! 请使用 local profile 启动: mvn spring-boot:run -Dspring-boot.run.profiles=local");
        } else {
            log.info("DeepSeek API Key 配置正常 ✓");
        }
        log.info("========================================");
    }

    // ==================== AI 对话接口 ====================

    /**
     * AI 对话 — 代理转发到 DeepSeek，注入 Function Calling 能力
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> requestBody) {
        // 检查 API Key 是否配置
        if (apiKey == null || apiKey.isEmpty() || "placeholder".equals(apiKey)) {
            return ResponseEntity.internalServerError()
                    .body(ApiResult.serverError("DeepSeek API Key 未配置，请使用 local profile 启动后端"));
        }
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");

            if (messages == null || messages.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.error(400, "消息列表不能为空"));
            }

            // 构建系统消息（前置插入）
            Map<String, Object> systemMsg = buildSystemMessage();
            List<Map<String, Object>> fullMessages = new ArrayList<>();
            fullMessages.add(systemMsg);
            fullMessages.addAll(messages);

            // 构建 Function Definitions
            List<Map<String, Object>> tools = buildToolDefinitions();

            // 多轮 Function Calling 循环（最多 5 轮，防止死循环）
            String response = null;
            int maxRounds = 5;
            for (int round = 0; round < maxRounds; round++) {
                // 只首轮带 tools，后续不带（避免 model 只调函数不说话）
                List<Map<String, Object>> currentTools = (round == 0) ? tools : Collections.emptyList();
                response = callDeepSeek(fullMessages, currentTools);

                JsonNode root = objectMapper.readTree(response);
                JsonNode choices = root.get("choices");
                if (choices == null || choices.size() == 0) break;

                JsonNode message = choices.get(0).get("message");
                JsonNode toolCalls = message.get("tool_calls");

                // 没有 tool_calls → 模型给出了最终文本回复，结束循环
                if (toolCalls == null || toolCalls.size() == 0) break;

                // 有 tool_calls → 执行函数，追加结果，继续下一轮
                log.info("===== Function Calling 第 {} 轮，共 {} 个调用 =====", round + 1, toolCalls.size());

                fullMessages.add(objectMapper.convertValue(message, Map.class));

                for (JsonNode tc : toolCalls) {
                    String callId = tc.get("id").asText();
                    String funcName = tc.get("function").get("name").asText();
                    String funcArgs = tc.get("function").get("arguments").asText();

                    log.info("  执行: {}({})", funcName, funcArgs);
                    String funcResult = executeFunction(funcName, funcArgs);

                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", callId);
                    toolMsg.put("content", funcResult);
                    fullMessages.add(toolMsg);
                }
            }

            return ResponseEntity.ok(ApiResult.success(objectMapper.readTree(response)));

        } catch (Exception e) {
            log.error("AI 对话异常", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResult.serverError("AI 服务异常: " + e.getMessage()));
        }
    }

    // ==================== 数据 API（供 Dashboard & Function Calling 使用） ====================

    /**
     * 获取最新预警结果
     */
    @GetMapping("/alerts/latest")
    public ApiResult<Map<String, Object>> getLatestAlerts(
            @RequestParam(required = false) String alertType) {
        List<AiAlert> alerts;
        try {
            if (alertType != null && !alertType.isEmpty()) {
                alerts = alertRepo.findByAlertTypeOrderByCreatedAtDesc(alertType);
            } else {
                alerts = alertRepo.findAllByOrderByCreatedAtDesc();
            }
        } catch (Exception e) {
            log.warn("查询AI预警记录失败（可能数据表尚未初始化）: {}", e.getMessage());
            alerts = Collections.emptyList();
        }

        // 按类型分组
        List<AiAlert> shortages = alerts.stream()
                .filter(a -> "SHORTAGE".equals(a.getAlertType()))
                .collect(Collectors.toList());
        List<AiAlert> deadStocks = alerts.stream()
                .filter(a -> "DEAD_STOCK".equals(a.getAlertType()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shortages", shortages);
        result.put("deadStocks", deadStocks);
        result.put("shortageCount", shortages.size());
        result.put("deadStockCount", deadStocks.size());
        result.put("highRiskCount", alerts.stream()
                .filter(a -> "HIGH".equals(a.getRiskLevel())).count());
        result.put("updatedAt", alerts.isEmpty() ? null : alerts.get(0).getCreatedAt());

        return ApiResult.success(result);
    }

    /**
     * 全量库存快照（供 Function Calling）
     */
    @GetMapping("/data/stocks")
    public ApiResult<List<InventoryStock>> getStocks() {
        return ApiResult.success(stockRepo.findAll());
    }

    /**
     * 出库历史（供 Function Calling）
     */
    @GetMapping("/data/outbound-history")
    public ApiResult<List<OutboundHistory>> getOutboundHistory(
            @RequestParam(required = false) String materialCode,
            @RequestParam(defaultValue = "50") int limit) {
        List<OutboundHistory> all = outboundHistoryRepo.findAll();
        if (materialCode != null && !materialCode.isEmpty()) {
            all = all.stream()
                    .filter(h -> materialCode.equals(h.getMaterialCode()))
                    .collect(Collectors.toList());
        }
        // 按时间倒序，取前 N 条
        all.sort((a, b) -> {
            LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
            return tb.compareTo(ta);
        });
        if (all.size() > limit) {
            all = all.subList(0, limit);
        }
        return ApiResult.success(all);
    }

    /**
     * 入库历史（供 Function Calling）
     */
    @GetMapping("/data/inbound-history")
    public ApiResult<List<Map<String, Object>>> getInboundHistory(
            @RequestParam(required = false) String materialCode,
            @RequestParam(defaultValue = "50") int limit) {
        List<InboundOrderDetail> all = inboundDetailRepo.findAll();
        if (materialCode != null && !materialCode.isEmpty()) {
            all = all.stream()
                    .filter(d -> materialCode.equals(d.getMaterialCode()))
                    .collect(Collectors.toList());
        }
        // 精简返回字段
        List<Map<String, Object>> result = new ArrayList<>();
        for (InboundOrderDetail d : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", d.getId());
            item.put("docNo", d.getDocNo());
            item.put("materialCode", d.getMaterialCode());
            item.put("materialName", d.getMaterialName());
            item.put("supplierName", d.getSupplierName());
            item.put("plannedQty", d.getPlannedQty());
            item.put("actualQty", d.getActualQty());
            item.put("warehouseArea", d.getWarehouseArea());
            item.put("createdAt", d.getCreatedAt());
            result.add(item);
        }
        result.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.getOrDefault("createdAt", LocalDateTime.MIN);
            LocalDateTime tb = (LocalDateTime) b.getOrDefault("createdAt", LocalDateTime.MIN);
            return tb.compareTo(ta);
        });
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        return ApiResult.success(result);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建系统消息，设定 AI 角色
     */
    private Map<String, Object> buildSystemMessage() {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "system");
        msg.put("content", "你是 WMS 仓库管理系统的 AI 仓库管理员助手。你的职责是：\n" +
                "1. 分析库存状态，回答缺货、呆滞等预警相关问题\n" +
                "2. 基于实时数据给仓库管理员提供决策建议\n" +
                "3. 当用户询问具体数据时，使用工具函数查询最新信息\n" +
                "4. 回答简洁、专业，用中文，使用具体数字\n" +
                "5. 如果数据不足，诚实告知");
        return msg;
    }

    /**
     * 构建 Function Calling 工具定义
     */
    private List<Map<String, Object>> buildToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 1. get_stocks — 查询全量库存
        Map<String, Object> stocksParams = new LinkedHashMap<>();
        stocksParams.put("type", "object");
        stocksParams.put("properties", new LinkedHashMap<>());
        tools.add(buildTool("get_stocks", "查询当前全量库存快照，返回所有物料的库存数量、库区、最后入库信息",
                stocksParams));

        // 2. get_outbound_history — 查询出库历史
        Map<String, Object> matCodeProp = new LinkedHashMap<>();
        matCodeProp.put("type", "string");
        matCodeProp.put("description", "物料号（可选，不传则查全部）");
        Map<String, Object> outboundProps = new LinkedHashMap<>();
        outboundProps.put("materialCode", matCodeProp);
        Map<String, Object> outboundParams = new LinkedHashMap<>();
        outboundParams.put("type", "object");
        outboundParams.put("properties", outboundProps);
        tools.add(buildTool("get_outbound_history",
                "查询出库历史记录，可按物料号筛选。返回出库单号、物料、出库数量、出库时间、来源批次等",
                outboundParams));

        // 3. get_inbound_history — 查询入库历史
        Map<String, Object> inboundProps = new LinkedHashMap<>();
        inboundProps.put("materialCode", matCodeProp);
        Map<String, Object> inboundParams = new LinkedHashMap<>();
        inboundParams.put("type", "object");
        inboundParams.put("properties", inboundProps);
        tools.add(buildTool("get_inbound_history",
                "查询入库历史记录，可按物料号筛选。返回入库单号、物料、入库数量、供应商、库区、入库时间等",
                inboundParams));

        // 4. get_latest_alerts — 查询最新预警
        Map<String, Object> alertTypeProp = new LinkedHashMap<>();
        alertTypeProp.put("type", "string");
        List<String> enumValues = new ArrayList<>();
        enumValues.add("SHORTAGE");
        enumValues.add("DEAD_STOCK");
        alertTypeProp.put("enum", enumValues);
        alertTypeProp.put("description", "预警类型：SHORTAGE(缺货) 或 DEAD_STOCK(呆滞)，不传则查全部");
        Map<String, Object> alertProps = new LinkedHashMap<>();
        alertProps.put("alertType", alertTypeProp);
        Map<String, Object> alertParams = new LinkedHashMap<>();
        alertParams.put("type", "object");
        alertParams.put("properties", alertProps);
        tools.add(buildTool("get_latest_alerts",
                "查询最新的 AI 预警分析结果，包括缺货预警和呆滞预警。可选按类型筛选",
                alertParams));

        return tools;
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        Map<String, Object> func = new LinkedHashMap<>();
        func.put("name", name);
        func.put("description", description);
        func.put("parameters", parameters);
        tool.put("function", func);
        return tool;
    }

    /**
     * 执行 Function Call
     */
    private String executeFunction(String functionName, String arguments) {
        try {
            switch (functionName) {
                case "get_stocks": {
                    List<InventoryStock> stocks = stockRepo.findAll();
                    // 返回精简 JSON 数组
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (InventoryStock s : stocks) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("materialCode", s.getMaterialCode());
                        item.put("materialName", s.getMaterialName());
                        item.put("supplier", s.getSupplier());
                        item.put("onHandQty", s.getOnHandQty());
                        item.put("warehouseArea", s.getWarehouseArea());
                        item.put("lastInboundAt", s.getLastInboundAt() != null ? s.getLastInboundAt().toString() : null);
                        items.add(item);
                    }
                    return objectMapper.writeValueAsString(items);
                }
                case "get_outbound_history": {
                    String materialCode = extractArg(arguments, "materialCode");
                    List<OutboundHistory> all = outboundHistoryRepo.findAll();
                    if (materialCode != null && !materialCode.isEmpty()) {
                        all = all.stream()
                                .filter(h -> materialCode.equals(h.getMaterialCode()))
                                .collect(Collectors.toList());
                    }
                    all.sort((a, b) -> {
                        LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                        return tb.compareTo(ta);
                    });
                    int limit = Math.min(all.size(), 30);
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (OutboundHistory h : all.subList(0, limit)) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("docNo", h.getDocNo());
                        item.put("materialCode", h.getMaterialCode());
                        item.put("materialName", h.getMaterialName());
                        item.put("issueQty", h.getIssueQty());
                        item.put("supplierName", h.getSupplierName());
                        item.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
                        items.add(item);
                    }
                    return objectMapper.writeValueAsString(items);
                }
                case "get_inbound_history": {
                    String materialCode = extractArg(arguments, "materialCode");
                    List<InboundOrderDetail> all = inboundDetailRepo.findAll();
                    if (materialCode != null && !materialCode.isEmpty()) {
                        all = all.stream()
                                .filter(d -> materialCode.equals(d.getMaterialCode()))
                                .collect(Collectors.toList());
                    }
                    all.sort((a, b) -> {
                        LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                        return tb.compareTo(ta);
                    });
                    int limit = Math.min(all.size(), 30);
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (InboundOrderDetail d : all.subList(0, limit)) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("docNo", d.getDocNo());
                        item.put("materialCode", d.getMaterialCode());
                        item.put("materialName", d.getMaterialName());
                        item.put("supplierName", d.getSupplierName());
                        item.put("plannedQty", d.getPlannedQty());
                        item.put("actualQty", d.getActualQty());
                        item.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
                        items.add(item);
                    }
                    return objectMapper.writeValueAsString(items);
                }
                case "get_latest_alerts": {
                    String alertType = extractArg(arguments, "alertType");
                    List<AiAlert> alerts;
                    if (alertType != null && !alertType.isEmpty()) {
                        alerts = alertRepo.findByAlertTypeOrderByCreatedAtDesc(alertType);
                    } else {
                        alerts = alertRepo.findAllByOrderByCreatedAtDesc();
                    }
                    return objectMapper.writeValueAsString(alerts);
                }
                default:
                    return "{\"error\": \"未知函数: " + functionName + "\"}";
            }
        } catch (Exception e) {
            log.error("执行 Function Call 失败: {}", functionName, e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 从 JSON 参数字符串中提取指定字段
     */
    private String extractArg(String arguments, String key) {
        try {
            JsonNode node = objectMapper.readTree(arguments);
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        } catch (JsonProcessingException ignored) {
        }
        return null;
    }

    /**
     * 调用 DeepSeek API
     */
    private String callDeepSeek(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        String url = baseUrl + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        // 只在有 tools 时才传，避免空数组导致模型行为异常
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }
        body.put("temperature", 0.7);
        body.put("max_tokens", 4000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("调用 DeepSeek API, messages 数量: {}", messages.size());
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("DeepSeek API 返回错误: " + response.getStatusCode());
        }

        return response.getBody();
    }
}
