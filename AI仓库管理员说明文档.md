# AI 仓库管理员 — 说明文档

> 版本：v1.0  
> 更新日期：2026-06-24  
> 所属项目：汽车物流 WMS 仓库管理系统

---

## 一、功能概述

「AI 仓库管理员」是 WMS 系统集成的智能助手模块，基于 **DeepSeek 大语言模型**，结合仓库实时数据，实现以下核心能力：

| 能力 | 说明 |
|:---|:---|
| **缺货预测** | 基于近 30 天出库消耗速率，预测每种物料还能支撑多少天，按 HIGH/MEDIUM/LOW 三级预警 |
| **呆滞报废预警** | 识别长期无出库记录的物料，按呆滞天数分级告警，防止资金占用和物料过期 |
| **自然语言对话** | 支持用日常语言提问，AI 自动查询数据库、分析趋势、生成建议 |
| **主动看板预警** | Dashboard 上直接展示最新的缺货/呆滞风险卡片，无需手动查询 |

---

## 二、功能入口

系统提供 **三个入口** 访问 AI 功能：

### 入口 1：Dashboard 预警卡片

> 路径：登录后首页仪表盘

在 Dashboard 页面中部，展示两张 AI 预警卡片：

- **AI 缺货预警**（左侧）：显示有缺货风险的物料 Top 5，标注风险等级（高风险/中风险/低风险）、当前库存、预估可支撑天数
- **AI 呆滞预警**（右侧）：显示有呆滞风险的物料 Top 5，标注呆滞天数和风险等级

点击任意物料行或「AI 分析」按钮，自动跳转到 AI 对话页面并带入该物料信息。

### 入口 2：全局悬浮按钮

> 位置：任意页面右下角

蓝色圆形悬浮按钮，点击弹出迷你聊天面板，可快速提问。特点：

- 不离开当前页面即可与 AI 对话
- 支持最多 5 轮连续对话
- 点击面板右上角全屏图标可跳转到完整对话页面
- 有高风险预警时按钮上显示红色小圆点

### 入口 3：AI 助手独立页面

> 路径：侧边栏 → AI 助手，或网址 `/ai/chat`

全屏对话页面，适合复杂分析和连续追问。提供：

- 快速提问标签（点击即可发送）
- 完整对话历史（同一次会话内）
- Markdown 富文本渲染（表格、标题、粗体、列表等）
- 清空对话按钮

---

## 三、核心功能详解

### 3.1 缺货预测

**工作原理：**

```
出库历史(近30天) → 计算日均消耗量 → 预估可支撑天数 → 三级风险定级 → 生成建议文案
```

| 风险等级 | 判定标准 | 标签颜色 | 建议动作 |
|:---|:---|:---|:---|
| **HIGH（高风险）** | 预计支撑 < 7 天 | 红色 | 立即安排补货 |
| **MEDIUM（中风险）** | 预计支撑 7~14 天 | 橙色 | 制定补货计划 |
| **LOW（低风险）** | 预计支撑 > 14 天 | 蓝色 | 库存充足，持续监控 |

**数据来源：** `outbound_history` 表的实际出库记录。如果某物料近 30 天无出库记录，则不会触发缺货预警。

### 3.2 呆滞报废预警

**工作原理：**

```
查询最后一次出库日期 → 计算呆滞天数 → 三级风险定级 → 生成处置建议
```

| 风险等级 | 判定标准 | 标签颜色 | 建议动作 |
|:---|:---|:---|:---|
| **HIGH（高风险）** | 呆滞 > 90 天 | 红色 | 评估报废或降价出清 |
| **MEDIUM（中风险）** | 呆滞 60~90 天 | 橙色 | 制定处置方案 |
| **LOW（低风险）** | 呆滞 30~60 天 | 蓝色 | 持续观察 |

**特殊情况：** 从未出库过的物料，以最后入库时间作为计时起点。

### 3.3 AI 对话助手

**支持的自然语言操作：**

| 提问示例 | AI 行为 |
|:---|:---|
| "当前库存情况怎么样？" | 自动调用 `get_stocks` 查询全量库存，生成汇总报告 |
| "控制器模块会不会缺货？" | 调用 `get_outbound_history` 查消耗趋势，计算可支撑天数 |
| "有没有呆滞物料？" | 调用 `get_latest_alerts` 查预警结果，列出风险物料 |
| "帮我分析 MAT-ELE-001 的消耗趋势" | 查该物料的出入库历史，分析消耗速率变化 |
| "对比宁波电子模组和苏州精密器具的供货情况" | 分别查询两家供应商的入库历史，对比分析 |

**AI 具备的查询能力（Function Calling）：**

| 函数名 | 功能 | 查询的数据表 |
|:---|:---|:---|
| `get_stocks` | 全量库存快照 | `inventory_stock` |
| `get_outbound_history` | 出库历史（支持按物料号筛选） | `outbound_history` |
| `get_inbound_history` | 入库历史（支持按物料号筛选） | `inbound_order_detail` |
| `get_latest_alerts` | 最新预警结果（支持按类型筛选） | `ai_alert` |

**多轮查询能力：** AI 可以分步查询——例如先查全局库存，发现某物料可疑后，再单独查其出库历史做深度分析。最多支持 5 轮连续查询。

---

## 四、定时预警机制

系统每小时自动执行一次全量分析，无需人工触发：

```
启动后延迟 30 秒 → 首次分析 → 每隔 1 小时重新分析
```

分析结果持久化存储在 `ai_alert` 表中，Dashboard 预警卡片直接读表，秒级响应。

---

## 五、技术架构

```
┌──────────────────────────────────────────────────┐
│                   Frontend (Vue 3)                 │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐  │
│  │Dashboard │  │ AI Chat 页面  │  │悬浮聊天按钮 │  │
│  │预警卡片   │  │              │  │            │  │
│  └────┬─────┘  └──────┬───────┘  └──────┬─────┘  │
│       └────────────────┼─────────────────┘        │
│                        │ /api/ai/*                │
└────────────────────────┼──────────────────────────┘
                         │
┌────────────────────────┼──────────────────────────┐
│                Backend (Spring Boot)               │
│  ┌────────────┐  ┌─────┴──────┐  ┌─────────────┐ │
│  │AiScheduler │  │AiChatCtrl  │  │AiAlertSvc   │ │
│  │(定时分析)   │  │(代理+FC)   │  │(计算引擎)    │ │
│  └─────┬──────┘  └─────┬──────┘  └──────┬──────┘ │
│        │               │                │         │
│        ▼               ▼                ▼         │
│  ┌──────────────────────────────────────────┐     │
│  │         JPA Repositories (数据层)          │     │
│  └──────────────────────────────────────────┘     │
│        │               │                           │
└────────┼───────────────┼───────────────────────────┘
         │               │
         ▼               ▼
    ┌───────┐    ┌──────────────┐
    │ MySQL │    │ DeepSeek API │
    │(wms_db)│   │(外部大模型)    │
    └───────┘    └──────────────┘
```

**数据流：**

1. **定时分析链路：** `AiScheduler` → `AiAlertService`（查库存+出库历史计算指标）→ 写入 `ai_alert` 表 → Dashboard 直接读取
2. **对话链路：** 前端发消息 → `AiChatController` → DeepSeek API（带 Function Definitions）→ DeepSeek 决定调哪个函数 → 后端执行 JPA 查询 → 结果返回 DeepSeek → DeepSeek 生成自然语言回复 → 前端渲染

---

## 六、配置说明

### 6.1 大模型配置

配置文件：`backend/src/main/resources/application-local.yml`

```yaml
deepseek:
  api-key: sk-xxxxxxxx    # DeepSeek API Key
  base-url: https://api.deepseek.com
  model: deepseek-v4-flash
```

### 6.2 预警周期调整

编辑 `backend/src/main/java/com/example/wms/config/AiScheduler.java`：

```java
@Scheduled(initialDelay = 30_000, fixedRate = 3_600_000)  // 1 小时
```

改为：

```java
@Scheduled(initialDelay = 30_000, fixedRate = 1_800_000)  // 30 分钟
```

### 6.3 消耗计算窗口调整

编辑 `backend/src/main/java/com/example/wms/service/AiAlertService.java`：

```java
private static final int CONSUMPTION_WINDOW_DAYS = 30;  // 改为 60 则基于近 60 天
```

---

## 七、数据库表

### ai_alert（AI 预警记录表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | BIGINT | 主键 |
| material_code | VARCHAR(50) | 物料号 |
| material_name | VARCHAR(100) | 物料名称 |
| alert_type | VARCHAR(20) | 预警类型：`SHORTAGE`（缺货）/ `DEAD_STOCK`（呆滞） |
| risk_level | VARCHAR(10) | 风险等级：`HIGH` / `MEDIUM` / `LOW` |
| current_stock | INT | 当前库存数量 |
| daily_consumption | DECIMAL(10,2) | 日均消耗量（仅缺货类型） |
| estimated_days | INT | 预估可支撑天数（仅缺货类型） |
| idle_days | INT | 呆滞天数（仅呆滞类型） |
| suggestion | TEXT | AI 生成的处理建议 |
| analysis_json | TEXT | 完整分析数据（JSON） |
| created_at | DATETIME | 分析时间 |

---

## 八、常见问题

### Q1：Dashboard 预警卡片为空？

**原因：** 系统刚启动，定时任务还未执行（首次延迟 30 秒），或库中没有足够的出入库历史数据。

**解决：** 等待 30 秒后刷新页面，或检查后端日志确认定时任务已执行。

### Q2：AI 对话返回"AI 服务异常"？

**可能原因：**
1. DeepSeek API Key 未配置或失效 → 检查后端启动日志中的配置诊断
2. 网络无法访问 `api.deepseek.com` → 检查网络代理
3. DeepSeek 服务端限流或欠费 → 登录 DeepSeek 控制台查看

### Q3：AI 回复内容不完整？

DeepSeek 非流式返回，完整响应时间取决于查询数据量，通常 3~10 秒。如遇超长内容被截断，可调整 `max_tokens` 参数（默认 4000）：

```java
// AiChatController.java 第 478 行
body.put("max_tokens", 4000);
```

### Q4：如何更换 AI 模型？

修改 `application-local.yml` 中的 `deepseek.model` 即可，例如：

```yaml
deepseek:
  model: deepseek-chat    # 或 deepseek-reasoner 等
```

### Q5：预警数据是否实时？

定时任务每小时刷新一次。如需立即刷新，重启后端或手动调用 `POST /api/ai/chat` 触发一次对话（对话过程中 AI 获取的是实时数据）。

---

## 九、文件清单

```
backend/src/main/java/com/example/wms/
├── entity/AiAlert.java              # AI 预警实体
├── repository/AiAlertRepository.java # 预警数据仓库
├── service/AiAlertService.java      # 预警计算引擎
├── controller/AiChatController.java  # AI 对话 + 数据API
└── config/AiScheduler.java          # 定时调度器

frontend/src/
├── views/ai/Chat.vue                # AI 对话全屏页面
├── components/ai/
│   ├── AiAlertCards.vue             # Dashboard 预警卡片
│   └── AiFloatingButton.vue        # 全局悬浮聊天按钮
└── api/ai.js                        # AI 接口封装
```

---

## 十、后续可扩展方向

- **SSE 流式响应**：目前是非流式（等全部返回），可改为 Server-Sent Events 逐字输出，提升对话体验
- **更多数据维度**：接入供应商及时率、物料合格率等指标，丰富分析能力
- **预警推送**：支持邮件/企业微信通知高风险预警
- **历史趋势图**：Dashboard 展示库存消耗趋势折线图
- **移动端适配**：悬浮按钮和对话页面适配手机屏幕
