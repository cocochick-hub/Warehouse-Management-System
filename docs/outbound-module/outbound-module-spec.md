# 出库管理模块 - 需求规格说明书

## 1. 概述

本文档定义仓库管理系统（WMS）出库管理模块的功能需求和技术规格。出库模块需要实现从创建出库单、执行出库（含先进先出分配）、扣减库存、更新看板状态到出库历史记录的完整业务流程。

## 2. 功能需求

### 2.1 出库单管理

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OB-001 | 创建出库单，支持多明细行（物料/供应商/数量） | P0 |
| OB-002 | 出库单列表查询，支持按单号/供应商/状态筛选和分页 | P0 |
| OB-003 | 查看出库单详情，包含头信息、明细行和关联库存 | P0 |
| OB-004 | 出库单状态流转：待出库 → 部分完成 → 已完成 | P0 |

### 2.2 出库执行（FIFO 先进先出）

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OB-005 | 出库时按先进先出原则自动分配库存批次 | P0 |
| OB-006 | 按入库时间升序消耗库存，最早入库的批次优先出库 | P0 |
| OB-007 | 同批次内按入库明细行号顺序消耗 | P0 |
| OB-008 | 单次出库可部分执行，支持多次出库直到完成 | P0 |

### 2.3 库存扣减

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OB-009 | 出库确认后自动扣减对应库存 | P0 |
| OB-010 | 当库存不足时拒绝出库并提示 | P0 |
| OB-011 | 事务保证：出库明细更新与库存扣减原子执行 | P0 |

### 2.4 出库历史

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OB-012 | 记录每次出库操作的明细（物料/批次/数量/时间/操作人） | P0 |
| OB-013 | 出库历史支持按出库单号查询 | P0 |
| OB-014 | 出库历史包含来源入库单号（追溯 FIFO 批次来源） | P1 |

### 2.5 看板/仪表盘集成

| 需求编号 | 需求描述 | 优先级 |
|---------|---------|-------|
| OB-015 | 出库完成后更新看板仪表盘待出库任务数 | P0 |
| OB-016 | 仪表盘统计卡片使用真实 API 数据（不再硬编码） | P0 |
| OB-017 | 待处理任务列表包含出库单任务 | P0 |

## 3. 数据模型

### 3.1 表：outbound_order_detail（新增）

```sql
CREATE TABLE outbound_order_detail (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    outbound_order_id   BIGINT       NOT NULL COMMENT '出库单ID',
    doc_no              VARCHAR(50)  NOT NULL COMMENT '出库单号冗余',
    line_no             INT          NOT NULL COMMENT '行号',
    supplier_code       VARCHAR(50)  NOT NULL COMMENT '需求方代码',
    supplier_name       VARCHAR(100) NOT NULL COMMENT '需求方名称',
    material_code       VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name       VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    planned_qty         INT          NOT NULL COMMENT '计划出库数量',
    actual_qty          INT          NOT NULL DEFAULT 0 COMMENT '累计实际出库数量',
    warehouse_area      VARCHAR(100) DEFAULT '默认库区' COMMENT '库区',
    remark              VARCHAR(255) DEFAULT NULL COMMENT '明细备注',
    created_by          VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by          VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_outbound_order_detail_order_id FOREIGN KEY (outbound_order_id) REFERENCES outbound_order (id),
    UNIQUE KEY uk_outbound_order_detail_order_supplier_material (outbound_order_id, supplier_code, material_code),
    KEY idx_outbound_order_detail_doc_no (doc_no),
    KEY idx_outbound_order_detail_material_code (material_code),
    KEY idx_outbound_order_detail_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';
```

### 3.2 表：outbound_history（新增 - 出库历史记录）

```sql
CREATE TABLE outbound_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    outbound_order_id   BIGINT       NOT NULL COMMENT '出库单ID',
    outbound_detail_id  BIGINT       NOT NULL COMMENT '出库明细ID',
    doc_no              VARCHAR(50)  NOT NULL COMMENT '出库单号',
    material_code       VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name       VARCHAR(100) NOT NULL COMMENT '物料名称',
    supplier_name       VARCHAR(100) NOT NULL COMMENT '需求方名称',
    issue_qty           INT          NOT NULL COMMENT '本次出库数量',
    source_inbound_doc  VARCHAR(50)  DEFAULT NULL COMMENT '来源入库单号(FIFO批次溯源)',
    source_detail_id    BIGINT       DEFAULT NULL COMMENT '来源入库明细ID',
    warehouse_area      VARCHAR(100) DEFAULT '默认库区' COMMENT '库区',
    issued_by           VARCHAR(50)  DEFAULT 'system' COMMENT '出库操作人',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出库时间',
    CONSTRAINT fk_outbound_history_order_id FOREIGN KEY (outbound_order_id) REFERENCES outbound_order (id),
    KEY idx_outbound_history_doc_no (doc_no),
    KEY idx_outbound_history_material_code (material_code),
    KEY idx_outbound_history_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库历史记录表';
```

### 3.3 表：outbound_order（扩展字段）

在现有 `outbound_order` 表基础上添加：

```sql
ALTER TABLE outbound_order 
    ADD COLUMN item_count        INT NOT NULL DEFAULT 0 COMMENT '明细条数',
    ADD COLUMN planned_total_qty INT NOT NULL DEFAULT 0 COMMENT '计划出库总数',
    ADD COLUMN actual_total_qty  INT NOT NULL DEFAULT 0 COMMENT '实际出库总数',
    ADD COLUMN remark            VARCHAR(255) DEFAULT NULL COMMENT '备注';
```

### 3.4 实体关系图

```
outbound_order (1) ──→ (N) outbound_order_detail
outbound_order_detail (1) ──→ (N) outbound_history
inbound_order_detail (1) ──→ (N) outbound_history (FIFO 溯源)
outbound_order_detail (N) ──→ (1) inventory_stock (扣减)
```

## 4. API 设计

### 4.1 出库单接口（`/api/outbound/orders`）

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/outbound/orders` | 查询出库单列表（分页+筛选） |
| GET | `/api/outbound/orders/{id}` | 获取出库单详情 |
| POST | `/api/outbound/orders` | 创建出库单 |
| POST | `/api/outbound/orders/{id}/issue` | 执行出库（FIFO分配+库存扣减） |

### 4.2 出库历史接口（`/api/outbound/history`）

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/outbound/history` | 查询出库历史（按单号筛选+分页） |

## 5. FIFO 算法设计

### 5.1 核心逻辑

```
FUNCTION allocateFIFO(materialCode, supplierName, requiredQty):
    result = []  // List of (inboundDetailId, inbDocNo, allocatedQty)
    remaining = requiredQty
    
    // 查询该物料+供应商的所有已入库明细，按创建时间升序
    inboundDetails = findInboundDetailsByMaterialAndSupplier(materialCode, supplierName)
                     .filter(detail → detail.inboundOrder.status == "已完成" 
                                    || detail.inboundOrder.status == "部分完成")
                     .sorted(by created_at ASC, line_no ASC)
    
    FOR EACH detail IN inboundDetails:
        availableQty = detail.actualQty - getConsumedQty(detail.id)
        IF availableQty <= 0: CONTINUE
        
        allocateQty = MIN(availableQty, remaining)
        result.add((detail.id, detail.docNo, allocateQty))
        remaining -= allocateQty
        
        IF remaining <= 0: BREAK
    
    IF remaining > 0:
        THROW "库存不足：{materialCode} 需要 {requiredQty}, 可用 {requiredQty - remaining}"
    
    RETURN result
```

### 5.2 已消耗数量计算

`getConsumedQty(detailId)` = 该入库明细在 `outbound_history` 表中所有出库数量之和。

## 6. 仪表盘集成

### 6.1 修改 DashboardController

`/api/dashboard/data` 接口需要返回真实统计数据：

| 字段 | 数据来源 |
|------|---------|
| pendingInbound | COUNT(inbound_order WHERE status != '已完成') |
| pendingOutbound | COUNT(outbound_order WHERE status != '已完成') |
| lowStockAlert | COUNT(inventory_stock WHERE on_hand_qty = 0) |
| totalMaterials | COUNT(DISTINCT material_code FROM material_info) |
| pendingTasks | 合并未完成的入库单和出库单，按创建时间倒序 |

### 6.2 修改 Dashboard.vue

- 移除硬编码的统计卡片值，改为从 API 获取
- 添加 `totalSuppliers` 统计供应商数量
- 添加 `totalOutboundToday` 统计今日出库数

## 7. 前端页面设计

### 7.1 出库单管理页 (`outbound/Order.vue`)

- 搜索栏：出库单号、需求方、状态筛选 + 创建按钮
- 表格列：单号、需求方、明细数、计划总数、实际总数、状态、创建时间
- 操作：查看详情、执行出库

### 7.2 创建出库单对话框（`OutboundOrderForm.vue`）

- 动态明细行：需求方、物料号、计划数量联动选择
- 校验：同一需求方+物料不能重复、至少一条明细

### 7.3 出库执行对话框（`OutboundIssueDialog.vue`）

- 显示明细行（物料/计划数/已出库数/可出库数）
- 输入本次出库数量
- 确认后自动 FIFO 分配

### 7.4 出库详情对话框（`OutboundOrderDetailDialog.vue`）

- 头信息展示
- 明细行列表
- 关联库存信息

### 7.5 出库历史页（`outbound/History.vue`）

- 搜索栏：出库单号筛选
- 表格列：出库时间、出库单号、物料、需求方、出库数量、来源入库单号、操作人

## 8. 非功能性需求

| 需求 | 描述 |
|------|------|
| 事务 | 出库执行过程使用 @Transactional，保证数据一致性 |
| 并发 | 同一出库单不允许并发执行出库操作 |
| 校验 | 前端+后端双重校验出库数量不超过计划数 |
| 错误处理 | 库存不足时返回明确错误信息 |

## 9. 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|---------|
| AC-1 | 创建出库单 | 成功创建，包含多行明细，自动生成单号 |
| AC-2 | 查询出库单列表 | 按条件筛选，分页正确 |
| AC-3 | 查看出库单详情 | 显示头信息+明细+库存 |
| AC-4 | 执行出库（FIFO） | 按先进先出分配，消耗最早入库批次 |
| AC-5 | 库存扣减 | 出库后库存数量正确减少 |
| AC-6 | 库存不足 | 拒绝出库并返回明确错误提示 |
| AC-7 | 看板统计 | 待出库数量正确反映在仪表盘 |
| AC-8 | 出库历史 | 完整记录每次出库的批次来源 |
| AC-9 | 状态流转 | 待出库 → 部分完成 → 已完成 正确流转 |
| AC-10 | 事务一致性 | 异常时数据回滚，不产生脏数据 |
