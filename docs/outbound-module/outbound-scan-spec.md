# 出库扫码模块 - 需求规格说明书

## 1. 概述

本文档定义仓库管理系统（WMS）出库扫码模块的功能需求和技术规格。出库扫码模块在现有出库单管理基础上，新增扫码出库、FIFO校验预警、出库单据打印和多库区出库四个功能特性，实现出库操作的扫码化、自动化。

## 2. 功能需求

### 2.1 条码扫码出库

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OS-001 | 操作员扫描出库单条码，系统解析并加载出库单信息 | P0 |
| OS-002 | 条码格式为 `WMS-OUTBOUND|<出库单号>`，前缀标识扫码类型 | P0 |
| OS-003 | 扫描后展示出库单头信息（单号/需求方/状态）及所有明细行 | P0 |
| OS-004 | 操作员选择明细行并输入本次出库数量，确认后执行出库 | P0 |
| OS-005 | 出库成功后自动刷新页面状态，支持连续扫码 | P0 |

### 2.2 FIFO 校验预警

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OS-006 | 出库执行前校验：检查是否存在更早批次的未消耗库存 | P0 |
| OS-007 | 若当前分配的批次不是最早批次，给出 FIFO 警告提示 | P0 |
| OS-008 | 警告信息包含：物料号、被跳过的批次信息（入库单号+入库时间） | P1 |
| OS-009 | 警告不阻断出库操作，操作员确认后可继续执行 | P1 |

### 2.3 打印出库单据

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OS-010 | 出库完成后提供"打印出库单"按钮 | P0 |
| OS-011 | 出库单据包含：出库单号、需求方、明细（物料/数量/批次）、操作人、时间 | P0 |
| OS-012 | 出库单据附带二维码，扫码可查看出库历史 | P1 |
| OS-013 | 使用浏览器原生打印功能，A4 纸张布局 | P0 |

### 2.4 多库区出库

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OS-014 | 出库执行时支持指定库区，一次出库可从多个库区扣减 | P0 |
| OS-015 | 出库请求支持明细行级别指定库区（`warehouse_area`） | P0 |
| OS-016 | FIFO 分配按库区内先进先出，不同库区独立分配 | P1 |
| OS-017 | 出库历史记录库区来源，支持按库区追溯 | P1 |

## 3. 数据模型

### 3.1 表：outbound_order_detail（扩展字段）

在现有 `outbound_order_detail` 表无需新增字段，`warehouse_area` 字段已存在。

### 3.2 表：outbound_order（扩展字段）

```sql
ALTER TABLE outbound_order 
    ADD COLUMN printed_at TIMESTAMP DEFAULT NULL COMMENT '打印时间';
```

### 3.3 表：outbound_history（扩展字段）

```sql
ALTER TABLE outbound_history 
    ADD COLUMN warehouse_area_index INT DEFAULT NULL COMMENT 'FIFO分配时库区内的批次序号（用于FIFO校验）';
```

### 3.4 出库扫码交互流程

```
扫描条码 → 解析出库单号 → GET /api/outbound/scan/{docNo} → 展示出库单详情
    → 输入出库数量 + 指定库区 → POST /api/outbound/scan/issue → FIFO校验+出库执行
    → 出库成功 → 可选打印出库单据 → 继续扫码
```

## 4. API 设计

### 4.1 出库扫码接口（`/api/outbound/scan`）

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/outbound/scan/{docNo}` | 扫描查询：根据出库单号获取出库单信息（含库存快照） |
| POST | `/api/outbound/scan/issue` | 扫码出库执行：FIFO分配 + FIFO校验预警 + 库存扣减 |

### 4.2 请求/响应结构

**GET `/api/outbound/scan/{docNo}` 响应（OutboundScanDTO）**：
```json
{
  "order": { "id": 1, "docNo": "OUT...", "supplier": "...", "status": "待出库" },
  "details": [
    {
      "detailId": 1, "materialCode": "MAT-001", "materialName": "...",
      "plannedQty": 100, "actualQty": 0, "warehouseArea": "默认库区"
    }
  ],
  "stocks": [
    { "materialCode": "MAT-001", "materialName": "...", "onHandQty": 50, "warehouseArea": "默认库区" }
  ]
}
```

**POST `/api/outbound/scan/issue` 请求（OutboundScanIssueRequest）**：
```json
{
  "docNo": "OUT20240623001",
  "details": [
    { "detailId": 1, "issueQty": 30, "warehouseArea": "默认库区" }
  ]
}
```

**POST `/api/outbound/scan/issue` 响应（OutboundScanIssueResponse）**：
```json
{
  "order": { ... },
  "details": [ ... ],
  "fifoWarnings": [
    { "materialCode": "MAT-001", "materialName": "...", "skippedBatch": "IN20240602001", "skippedInboundTime": "2024-06-02T10:00:00" }
  ],
  "historyIds": [1, 2]
}
```

## 5. FIFO 校验预警算法

### 5.1 核心逻辑

```
FUNCTION checkFIFOOrder(materialCode, supplierName, warehouseArea, allocatedDetails):
    // 查询该物料+供应商+库区所有已入库明细，按入库时间升序
    allInbound = findInboundDetails(materialCode, supplierName, warehouseArea)
    
    // 计算每次入库明细的剩余可用量
    FOR EACH inbound IN allInbound:
        consumed = SUM(outbound_history.issueQty WHERE source_detail_id = inbound.id)
        available = inbound.actualQty - consumed
        
    // 找出本次分配的最早批次
    allocatedInboundIds = allocatedDetails.map(d -> d.sourceDetailId)
    earliestAllocatedIndex = min(allInbound.indexWhere(id in allocatedInboundIds))
    
    // 检查是否有更早批次未被消耗
    warnings = []
    FOR i FROM 0 TO earliestAllocatedIndex - 1:
        IF available[i] > 0:
            warnings.add({materialCode, skippedBatch: allInbound[i].docNo, skippedTime: allInbound[i].createdAt})
    
    RETURN warnings
```

### 5.2 触发时机

- 每次扫码出库执行前触发校验
- 仅当存在更早批次仍有库存但未被分配时产生警告
- 警告返回前端展示，不阻断出库流程

## 6. 前端页面设计

### 6.1 扫码出库页（`outbound/Scan.vue`）

- 扫码输入区：输入框（支持扫码枪、手动输入、粘贴），按回车查询
- 出库单信息展示：单号、需求方、状态、明细表格
- 明细表格列：物料号、物料名称、计划数、已出库数、本次出库数（可编辑）、库区（可选择）
- 操作区：确认出库按钮、清空按钮
- 提示区：FIFO 警告信息（黄色警告条）
- 操作提示：说明条码格式和操作步骤

### 6.2 打印出库单对话框（`OutboundPrintDialog.vue`）

- 打印预览区：A4 纸张布局的出库单据
- 内容：出库单号、需求方、日期、明细表格（物料/数量/批次来源）、操作人
- 二维码：指向出库历史查询链接
- 操作按钮：打印按钮

## 7. 非功能性需求

| 需求 | 描述 |
|------|------|
| 事务 | 扫码出库执行使用 @Transactional，保证明细更新、库存扣减、历史记录原子性 |
| 连续扫码 | 出库成功后自动清空输入框聚焦，不阻断后续扫描 |
| 错误处理 | 出库单不存在、已完成的单、库存不足均返回明确错误信息 |
| 多库区 | 同一出库明细可从不同库区扣减，库区间 FIFO 独立 |

## 8. 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|---------|
| AC-1 | 扫描出库单条码 | 成功解析并加载出库单信息 |
| AC-2 | 扫码出库执行 | FIFO 分配库存、扣减、生成历史记录 |
| AC-3 | FIFO 预警 | 跳批时显示黄色警告，提示被跳过的批次 |
| AC-4 | 打印出库单据 | 弹窗展示完整出库单信息，打印正常 |
| AC-5 | 多库区出库 | 不同库区独立扣减库存，历史记录标注库区 |
| AC-6 | 库存不足 | 返回明确错误提示，事务回滚 |
| AC-7 | 连续扫码 | 出库成功后自动准备下次扫码 |
