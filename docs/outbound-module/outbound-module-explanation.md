# 出库管理模块 - 实现说明文档

## 1. 模块概述

出库管理模块是 WMS 仓库管理系统的核心模块之一，实现了从创建出库单、基于**先进先出（FIFO）**原则分配库存批次、扣减库存、记录出库历史到更新看板仪表盘的完整出库业务流程。

### 核心功能

| 功能 | 描述 |
|------|------|
| 出库单管理 | 创建、查询、查看出库单，支持多需求方多物料明细 |
| FIFO 出库 | 按入库时间升序消耗库存，最早入库的批次优先出库 |
| 库存扣减 | 出库确认后自动扣减 `inventory_stock` 表库存量 |
| 出库历史 | 记录每次出库的批次来源、时间、操作人，支持溯源 |
| 看板集成 | 仪表盘统计数据使用真实 API 数据 |

---

## 2. 系统架构

### 2.1 层次架构

```
前端 (Vue 3 + Element Plus)
  ├── views/outbound/      页面级组件
  │   ├── Order.vue        出库单管理
  │   └── History.vue      出库历史
  ├── components/outbound/ 对话框组件
  │   ├── OutboundOrderForm.vue       创建出库单
  │   ├── OutboundIssueDialog.vue     执行出库
  │   └── OutboundOrderDetailDialog.vue 详情查看
  └── api/outbound.js      API 封装

后端 (Spring Boot 2.7 + JPA)
  ├── Controller  → OutboundOrderController.java
  ├── Service     → OutboundOrderServiceImpl.java
  ├── Repository  → OutboundOrderRepository, OutboundOrderDetailRepository, OutboundHistoryRepository
  ├── Entity      → OutboundOrder, OutboundOrderDetail, OutboundHistory
  └── DTO         → 9 个 DTO 类

数据库 (MySQL)
  ├── outbound_order        出库单头表
  ├── outbound_order_detail 出库明细表
  └── outbound_history      出库历史表（批次溯源）
```

### 2.2 数据流

```
创建出库单:  前端 → POST /api/outbound/orders → 写入 outbound_order + outbound_order_detail
                    ↓
执行出库(FIFO):  前端 → POST /api/outbound/orders/{id}/issue
                    ↓
               查询可用的入库明细(按时间升序) → calculate consumedQty from outbound_history
                    ↓
               按 FIFO 逐批分配 → 创建 outbound_history 记录
                    ↓
               扣减 inventory_stock.on_hand_qty
                    ↓
               更新 outbound_order.status
```

---

## 3. FIFO 先进先出算法详解

### 3.1 算法核心逻辑

FIFO 算法位于 `OutboundOrderServiceImpl.allocateFIFO()` 方法中：

```
输入: materialCode (物料号), supplierName (供应商), requiredQty (需要出库数量)
输出: List<FifoAllocation> (分配结果列表)

步骤:
1. 查询该物料+供应商的所有入库明细 (inbound_order_detail)
2. 按 createdAt ASC, lineNo ASC 排序（最早入库的优先）
3. 对每个入库明细:
   a. 计算已消耗数量 = SUM(outbound_history.issue_qty WHERE source_detail_id = 当前明细ID)
   b. 可用数量 = 入库明细的 actual_qty - 已消耗数量
   c. 本次分配 = MIN(可用数量, 剩余需要量)
   d. 剩余需要量 -= 本次分配
   e. 如果剩余需要量 <= 0，停止分配
4. 如果剩余需要量 > 0，抛出库存不足异常
5. 返回分配结果列表
```

### 3.2 关键设计：已消耗数量追踪

系统通过 `outbound_history` 表追踪每个入库明细的已消耗数量：

```
inbound_order_detail (id=10, actual_qty=100)
  ├── outbound_history: source_detail_id=10, issue_qty=30  (第一次出库)
  ├── outbound_history: source_detail_id=10, issue_qty=20  (第二次出库)
  └── 已消耗 = 30 + 20 = 50, 可用 = 100 - 50 = 50
```

这种方式避免了在入库明细上维护额外的"已消耗"字段，确保了数据的不可变性。

### 3.3 示例

假设物料 `MAT-ELE-001` 有三次入库记录：

| 入库明细 ID | 入库单号 | 入库数量 | 入库时间 |
|------------|---------|---------|---------|
| 3 | IN20240601002 | 30 | 2026-06-07 |
| 5 | IN20240603001 | 50 | 2026-06-10 |
| 7 | IN20240605001 | 40 | 2026-06-15 |

现在需要出库 55 个：

1. 分配批次 1 (ID=3): 30 个全部分配 → 剩余 25
2. 分配批次 2 (ID=5): 分配 25 个 → 剩余 0
3. 批次 3 (ID=7) 未被消耗

出库历史记录：
- `{ sourceInboundDoc: "IN20240601002", sourceDetailId: 3, issueQty: 30 }`
- `{ sourceInboundDoc: "IN20240603001", sourceDetailId: 5, issueQty: 25 }`

---

## 4. 数据库设计

### 4.1 出库单头表 (outbound_order)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| doc_no | VARCHAR(50) UK | 出库单号 (OUT+时间戳) |
| supplier | VARCHAR(100) | 需求方名称 |
| status | VARCHAR(20) | 待出库 / 部分完成 / 已完成 |
| item_count | INT | 明细条数 |
| planned_total_qty | INT | 计划出库总数 |
| actual_total_qty | INT | 实际出库总数 |
| remark | VARCHAR(255) | 备注 |

### 4.2 出库明细表 (outbound_order_detail)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| outbound_order_id | BIGINT FK | 关联出库单 |
| doc_no | VARCHAR(50) | 出库单号冗余 |
| line_no | INT | 行号 |
| supplier_code | VARCHAR(50) | 需求方代码 |
| supplier_name | VARCHAR(100) | 需求方名称 |
| material_code | VARCHAR(50) | 物料号 |
| material_name | VARCHAR(100) | 物料名称 |
| planned_qty | INT | 计划出库数量 |
| actual_qty | INT | 累计实出数量 |
| warehouse_area | VARCHAR(100) | 库区 |

唯一约束: `(outbound_order_id, supplier_code, material_code)` 防止同一出库单内重复的物料。

### 4.3 出库历史表 (outbound_history)

这是 FIFO 批次溯源的核心表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| outbound_order_id | BIGINT FK | 出库单 ID |
| outbound_detail_id | BIGINT | 出库明细 ID |
| doc_no | VARCHAR(50) | 出库单号 |
| material_code | VARCHAR(50) | 物料号 |
| material_name | VARCHAR(100) | 物料名称 |
| supplier_name | VARCHAR(100) | 需求方 |
| **issue_qty** | INT | **本次出库数量** |
| **source_inbound_doc** | VARCHAR(50) | **来源入库单号** (FIFO溯源) |
| **source_detail_id** | BIGINT | **来源入库明细 ID** (FIFO溯源) |
| warehouse_area | VARCHAR(100) | 库区 |
| issued_by | VARCHAR(50) | 操作人 |
| created_at | DATETIME | 出库时间 |

### 4.4 库存表更新 (inventory_stock)

出库时不创建新记录，而是扣减现有记录的 `on_hand_qty`：

```java
stock.setOnHandQty(stock.getOnHandQty() - issueQty);
```

唯一键 `(material_code, supplier)` 确保每个物料-供应商组合最多有一条库存记录。

---

## 5. API 接口说明

### 5.1 查询出库单列表

```
GET /api/outbound/orders?docNo=&supplier=&status=&page=1&size=10
```

响应:
```json
{
  "code": 200,
  "data": {
    "total": 2,
    "page": 1,
    "size": 10,
    "records": [{
      "id": 1,
      "docNo": "OUT20240601001",
      "supplier": "多需求方",
      "status": "待出库",
      "itemCount": 2,
      "plannedTotalQty": 80,
      "actualTotalQty": 0,
      "remark": null,
      "createdAt": "2026-06-15T10:00:00"
    }]
  }
}
```

### 5.2 获取出库单详情

```
GET /api/outbound/orders/{id}
```

响应包含 `order`（头信息）、`details`（明细列表）、`stocks`（当前库存）。

### 5.3 创建出库单

```
POST /api/outbound/orders
Content-Type: application/json

{
  "supplier": "多需求方",
  "remark": "紧急出库",
  "details": [{
    "supplierCode": "SUP-003",
    "supplierName": "宁波电子模组",
    "materialCode": "MAT-ELE-001",
    "materialName": "控制器模块",
    "plannedQty": 50,
    "warehouseArea": "默认库区"
  }]
}
```

### 5.4 执行出库

```
POST /api/outbound/orders/{id}/issue
Content-Type: application/json

{
  "details": [
    { "detailId": 1, "issueQty": 30 },
    { "detailId": 2, "issueQty": 20 }
  ]
}
```

此接口在 `@Transactional` 保护下原子执行 FIFO 分配、库存扣减、历史记录写入。

### 5.5 查询出库历史

```
GET /api/outbound/history?docNo=&page=1&size=10
```

响应:
```json
{
  "code": 200,
  "data": {
    "content": [{
      "id": 1,
      "docNo": "OUT20240601001",
      "materialCode": "MAT-ELE-001",
      "materialName": "控制器模块",
      "supplierName": "宁波电子模组",
      "issueQty": 30,
      "sourceInboundDoc": "IN20240601002",
      "warehouseArea": "默认库区",
      "issuedBy": "admin",
      "createdAt": "2026-06-15T12:00:00"
    }],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

### 5.6 错误响应

| 场景 | HTTP 状态 | 错误信息 |
|------|----------|---------|
| 库存不足 | 500 | `库存不足: 物料 MAT-ELE-001 需要 100, 可用 30, 缺少 70` |
| 已完成单据再次出库 | 500 | `已完成单据不允许再次出库` |
| 出库数量超限 | 500 | `物料 MAT-ELE-001 出库数量不能大于待出库数量` |

---

## 6. 出库流程详解

### 6.1 创建出库单

```
1. 前端选择需求方和物料，输入计划出库数量
2. 后端验证:
   - 至少一条明细
   - 同一需求方+物料不能重复
   - 计划数量 > 0
3. 生成出库单号: OUT + yyyyMMddHHmmssSSS + 3位序号
4. 自动判定需求方:
   - 只有一种需求方 → 直接使用
   - 多种需求方 → "多需求方"
5. 写入 outbound_order + outbound_order_detail
6. 状态: 待出库
```

### 6.2 执行出库

```
1. 前端选择出库单，填入本次出库数量
2. 后端执行（@Transactional 事务保护）:
   a. 校验出库数量不超过待出库数量
   b. 调用 allocateFIFO 进行 FIFO 分配
   c. 创建 outbound_history 记录（含批次来源）
   d. 调用 deductInventory 扣减库存
   e. 更新出库明细 actual_qty
   f. 更新出库单 status（待出库 → 部分完成 → 已完成）
3. 如有任何异常，全部回滚
```

### 6.3 状态流转

```
待出库 (actual=0) ──→ 部分完成 (0 < actual < planned) ──→ 已完成 (actual >= planned)
```

---

## 7. 前端页面说明

### 7.1 出库单管理页 (`views/outbound/Order.vue`)

- **搜索栏**: 按出库单号、需求方、状态筛选
- **表格**: 显示单号、需求方、明细数、计划总数、实出总数、状态（带颜色标签）、创建时间
- **操作按钮**:
  - 详情：查看出库单完整信息和关联库存
  - 出库：打开执行出库对话框（已完成状态禁用）

### 7.2 创建出库单对话框 (`OutboundOrderForm.vue`)

- 动态明细表格，支持级联选择需求方 → 物料
- 自动加载供应商和物料列表
- 校验：禁止重复的需求方+物料组合
- 提交后调用 `createOrder` API 并刷新列表

### 7.3 执行出库对话框 (`OutboundIssueDialog.vue`)

- 显示出库单头信息
- 明细表格显示：计划出库、已出库、待出库、本次出库（输入框）
- 只显示有待出库数量的明细行
- 验证：至少一条出库数量 > 0，每行不超过待出库数
- 提交后调用 `issueOrder` API

### 7.4 出库详情对话框 (`OutboundOrderDetailDialog.vue`)

- 头信息：单号、需求方、状态、明细数、计划/实出总数、备注
- 明细表：行号、需求方、物料、计划数、实出数、待出库、库区
- 库存表：物料号、名称、供应商、库存数量、最近入库信息

### 7.5 出库历史页 (`views/outbound/History.vue`)

- 按出库单号筛选
- 表格列：出库时间、出库单号、物料、需求方、出库数量、**来源入库单号（批次溯源）**、库区、操作人

### 7.6 仪表盘更新 (`views/Dashboard.vue`)

- 统计卡片使用 API 真实数据
- `pendingOutbound` 来自 `outboundOrderRepository.countByStatusNot("已完成")`
- 待办任务列表包含出库单

---

## 8. 关键代码片段

### 8.1 FIFO 分配核心方法

```java
// OutboundOrderServiceImpl.java:289-326
private List<FifoAllocation> allocateFIFO(String materialCode, String supplierName, int requiredQty) {
    List<FifoAllocation> result = new ArrayList<>();
    int remaining = requiredQty;

    // 查询入库明细，按创建时间升序排列（FIFO）
    List<InboundOrderDetail> inboundDetails = inboundOrderDetailRepository
            .findByMaterialCodeAndSupplierName(materialCode, supplierName);

    inboundDetails = inboundDetails.stream()
            .filter(d -> d.getInboundOrderId() != null)
            .sorted(Comparator
                .comparing((InboundOrderDetail d) -> 
                    d.getCreatedAt() == null ? LocalDateTime.MIN : d.getCreatedAt())
                .thenComparing(d -> safeInt(d.getLineNo())))
            .collect(Collectors.toList());

    for (InboundOrderDetail inboundDetail : inboundDetails) {
        if (remaining <= 0) break;

        // 通过 outbound_history 计算已消耗数量
        int consumedQty = outboundHistoryRepository
            .findBySourceDetailId(inboundDetail.getId())
            .stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();

        int availableQty = safeInt(inboundDetail.getActualQty()) - consumedQty;
        if (availableQty <= 0) continue;

        int allocateQty = Math.min(availableQty, remaining);
        result.add(new FifoAllocation(inboundDetail.getId(), 
            inboundDetail.getDocNo(), allocateQty));
        remaining -= allocateQty;
    }

    if (remaining > 0) {
        throw new IllegalStateException("库存不足: 物料 " + materialCode 
            + " 需要 " + requiredQty + ", 可用 " + (requiredQty - remaining) 
            + ", 缺少 " + remaining);
    }

    return result;
}
```

### 8.2 库存扣减方法

```java
// OutboundOrderServiceImpl.java:328-345
private void deductInventory(List<OutboundHistory> histories, 
                              String operator, LocalDateTime now) {
    for (OutboundHistory history : histories) {
        InventoryStock stock = inventoryStockRepository
                .findByMaterialCodeAndSupplier(
                    history.getMaterialCode(), history.getSupplierName())
                .orElse(null);
        if (stock == null) continue;

        stock.setOnHandQty(safeInt(stock.getOnHandQty()) - history.getIssueQty());
        if (stock.getOnHandQty() < 0) stock.setOnHandQty(0);
        stock.setUpdatedBy(operator);
        stock.setUpdatedAt(now);
        inventoryStockRepository.save(stock);
    }
}
```

### 8.3 事务保护

```java
@Override
@Transactional  // 确保 FIFO 分配 + 库存扣减 + 历史写入的原子性
public OutboundOrderDetailResponse issueOrder(Long id, 
        OutboundIssueRequest request, String operator) {
    // ... 完整的出库执行逻辑
}
```

---

## 9. 验证与测试

### 9.1 编译验证

```bash
# 后端编译
cd backend && mvn compile -q    # PASSED

# 前端构建
cd frontend && npm run build    # PASSED (4.57s)
```

### 9.2 功能测试场景

| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 创建出库单 | 创建含 2 条明细的出库单 | 单号 OUT 开头，状态 "待出库" |
| 查询列表 | 搜索栏筛选 | 按条件正确筛选并分页 |
| 查看详情 | 点击 "详情" | 显示头信息、明细、库存 |
| 执行出库 | 输入出库数量，确认 | FIFO 分配，库存扣减，历史记录生成 |
| 库存不足 | 出库数量超过可用库存 | 返回错误 "库存不足" |
| 部分出库 | 出库 30/50 | 状态变为 "部分完成" |
| 完成出库 | 补出剩余 20/50 | 状态变为 "已完成" |
| 已完成再出库 | 对已完成单据执行出库 | 拒绝 "已完成单据不允许再次出库" |
| 出库历史 | 查询历史记录 | 显示批次来源入库单号 |
| 看板统计 | 查看仪表盘 | 待出库数正确反映 |

### 9.3 端到端验证

由于环境限制（无 MySQL 实例），完整的端到端测试需要在部署环境中执行。代码层面已通过以下验证：

1. ✅ 后端 `mvn compile -q` 零错误编译通过
2. ✅ 前端 `npm run build` 成功构建
3. ✅ 所有 Entity/DTO 字段与数据库表定义一致
4. ✅ Repository 方法与查询需求匹配
5. ✅ Service 层事务边界正确配置
6. ✅ Controller 端点路径与前端 API 调用一致
7. ✅ Router 包含 `outbound/history` 路由
8. ✅ Sidebar 包含 "出库历史" 菜单项
9. ✅ Dashboard 使用 API 数据而非硬编码

---

## 10. 与入库模块的对比

| 特性 | 入库模块 | 出库模块 |
|------|---------|---------|
| 单号前缀 | IN | OUT |
| 状态 | 未入库→部分完成→已完成 | 待出库→部分完成→已完成 |
| 库存操作 | 增加 `on_hand_qty` | 减少 `on_hand_qty` |
| 批次分配 | 不涉及 | **FIFO 先进先出** |
| 历史追踪 | 看板标签（QR码） | **outbound_history（批次溯源）** |
| 扫描功能 | 支持扫码入库 | 不支持（按需求） |
| 多供应商 | 支持 "多供应商" | 支持 "多需求方" |

### 架构决策差异

1. **FIFO 算法**: 出库模块的核心创新点。通过 `outbound_history.source_detail_id` 追溯每个入库批次的消耗情况，而非在入库明细上直接维护已消耗字段。这保证了数据的不可变性，方便审计。

2. **历史表设计**: 出库历史表不继承 `BaseEntity`，而是包含自己的 `created_at` 和 `issued_by` 字段。这因为每次出库都生成多条历史记录（每条对应一个 FIFO 批次分配），需要记录精确的出库时间，而非通用的创建/更新时间。

3. **库存操作方向**: 入库增加库存、出库减少库存，但两者的幂等性要求不同。出库在扣减时添加了负值保护（不会低于 0）。

---

## 11. 文件清单

### 新建文件 (17 个)

**后端 (13 个)**:
- `entity/OutboundOrderDetail.java`
- `entity/OutboundHistory.java`
- `dto/outbound/OutboundOrderSummaryDTO.java`
- `dto/outbound/OutboundOrderDetailDTO.java`
- `dto/outbound/OutboundOrderCreateRequest.java`
- `dto/outbound/OutboundCreateDetailRequest.java`
- `dto/outbound/OutboundIssueRequest.java`
- `dto/outbound/OutboundIssueDetailRequest.java`
- `dto/outbound/OutboundOrderPageResponse.java`
- `dto/outbound/OutboundOrderDetailResponse.java`
- `dto/outbound/OutboundHistoryDTO.java`
- `repository/OutboundOrderDetailRepository.java`
- `repository/OutboundHistoryRepository.java`
- `service/OutboundOrderService.java`
- `service/impl/OutboundOrderServiceImpl.java`
- `controller/OutboundOrderController.java`

**前端 (4 个)**:
- `api/outbound.js`
- `components/outbound/OutboundOrderForm.vue`
- `components/outbound/OutboundIssueDialog.vue`
- `components/outbound/OutboundOrderDetailDialog.vue`
- `views/outbound/History.vue`

### 修改文件 (10 个)

**后端**: `OutboundOrder.java`, `OutboundOrderRepository.java`, `InboundOrderDetailRepository.java`, `InboundOrderRepository.java`, `InventoryStockRepository.java`, `DashboardServiceImpl.java`, `schema.sql`

**前端**: `views/outbound/Order.vue`, `views/Dashboard.vue`, `router/index.js`, `layout/Sidebar.vue`
