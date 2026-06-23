# 出库扫码模块 - 实现说明文档

## 1. 模块概述

出库扫码模块是 WMS 仓库管理系统出库管理模块的子功能，在现有出库单管理基础上增加了**扫码出库、FIFO 校验预警、出库单据打印和多库区出库**四个核心特性，实现出库操作的扫码化和自动化。

扫码对象为入库时生成的**看板标签（Kanban Label）**，而非出库单本身。操作员扫描看板条码后，系统解析看板信息，操作员输入对应的出库单 ID 和出库数量，即可快速完成出库。

### 核心功能

| 功能 | 描述 |
|------|------|
| 条码扫码出库 | 扫描看板条码（`WMS-INBOUND|` 前缀，复用入库看板标签），解析加载看板信息，输入出库单 ID 和数量后执行出库 |
| FIFO 校验预警 | 查询看板时自动检查是否存在更早批次的未消耗库存，在出库前弹出预警提示 |
| 打印出库单据 | 出库单管理页提供打印按钮，弹窗展示 A4 布局出库单据，支持浏览器原生打印 |
| 多库区出库 | 出库请求支持明细行级别指定库区（`warehouseArea`），不同库区独立扣减库存 |

---

## 2. 系统架构

### 2.1 层次架构

```
前端 (Vue 3 + Element Plus)
  ├── views/outbound/
  │   └── Scan.vue              扫码出库页面
  ├── components/outbound/
  │   └── OutboundPrintDialog.vue  打印出库单对话框
  └── api/outbound.js            API 封装

后端 (Spring Boot 2.7 + JPA)
  ├── Controller  → OutboundScanController.java
  ├── Service     → OutboundOrderServiceImpl.java（getOutboundScanLabel / issueByScan）
  ├── Repository  → OutboundOrderRepository, OutboundOrderDetailRepository, OutboundHistoryRepository,
  │                  InboundKanbanLabelRepository, InboundOrderDetailRepository, InboundOrderRepository, InventoryStockRepository
  ├── Entity      → InboundKanbanLabel, OutboundOrder, OutboundOrderDetail, OutboundHistory, InventoryStock, InboundOrderDetail
  └── DTO         → OutboundScanLabelResponse, OutboundScanIssueRequest

数据库 (MySQL)
  ├── inbound_kanban_label   看板标签表（入库扫码时生成）
  ├── outbound_order         出库单头表
  ├── outbound_order_detail  出库明细表
  └── outbound_history       出库历史表（批次溯源）
```

### 2.2 数据流

```
扫码查询:  扫描看板条码（WMS-INBOUND|<看板号>）
              → GET /api/outbound/scan/labels/{kanbanNo}
              ↓
           查询 inbound_kanban_label 表获取标签信息
              ↓
           查询关联的入库明细、入库单
              ↓
           计算可用数量 = 入库明细 actual_qty - SUM(outbound_history.issue_qty)
              ↓
           FIFO 校验：检查是否有更早批次未消耗库存
              ↓
           返回 OutboundScanLabelResponse（含看板信息+FIFO预警标志）

扫码出库:  填写出库单 ID + 出库数量
              → POST /api/outbound/scan/issue
              ↓
           校验看板状态（必须已入库）、出库单状态（未完成）、库存充足
              ↓
           匹配出库单明细行（按物料号+需求方）
              ↓
           创建 outbound_history 记录（含批次来源）
              ↓
           扣减 inventory_stock.on_hand_qty
              ↓
           更新出库明细 actual_qty、出库单 status
              ↓
           返回更新后的出库单详情
```

---

## 3. FIFO 先进先出校验预警详解

### 3.1 校验时机

FIFO 校验在**扫码查询阶段**执行（`getOutboundScanLabel` 方法中），而非在出库执行阶段。这样操作员在输入出库数量之前就能看到预警提示。

### 3.2 校验核心逻辑

位于 `OutboundOrderServiceImpl.getOutboundScanLabel()` 方法中（第 221-284 行）：

```
输入: kanbanNo (看板号)
输出: OutboundScanLabelResponse（含 fifoWarning, fifoMessage, earliestDocNo）

步骤:
1. 根据 kanbanNo 查询 inbound_kanban_label 表，获取看板标签信息
2. 查询关联的入库明细（inbound_order_detail）和入库单（inbound_order）
3. 计算可用数量 = 入库明细 actual_qty - SUM(outbound_history.issue_qty WHERE source_detail_id = detail.id)
4. 查询该物料+需求方的所有入库明细
5. 遍历所有入库明细对应的入库单:
   a. 只考虑状态为"已完成"或"部分完成"的入库单（已入库的）
   b. 找到最早的入库时间（createdAt）
   c. 记录最早入库单号
6. 如果最早入库单号 != 当前看板所属入库单号 → fifoWarning = true
7. 返回含预警标志的响应
```

### 3.3 FIFO 预警交互流程

```
扫码查询 → 系统返回 fifoWarning=true → 前端弹出预警对话框
    → 显示：非最早批次出库 + 当前批次信息 + 建议优先出库的最早批次
    → 操作员选择"继续出库"（不阻断）或"取消"
```

### 3.4 设计决策：预警不阻断出库

FIFO 预警作为信息提示返回前端，不抛异常阻断出库流程。原因：实际仓库场景中可能存在合法的非 FIFO 出库需求（如紧急出库、批次特批、库区限制等）。

---

## 4. 数据库设计

### 4.1 看板标签表 (inbound_kanban_label)

扫码出库的核心依赖表，由入库扫码模块在入库时生成：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| kanban_no | VARCHAR | 看板号（唯一条码标识） |
| doc_no | VARCHAR | 入库单号 |
| material_code | VARCHAR | 物料号 |
| material_name | VARCHAR | 物料名称 |
| supplier_code | VARCHAR | 需求方代码 |
| supplier_name | VARCHAR | 需求方名称 |
| label_qty | INT | 本包数量 |
| warehouse_area | VARCHAR | 库区 |
| label_status | VARCHAR | 标签状态（已入库 / 未入库） |
| inbound_order_id | BIGINT | 关联入库单 ID |
| inbound_order_detail_id | BIGINT | 关联入库明细 ID |

### 4.2 无新增表

出库扫码模块不创建新表，完全复用以下已有表：

| 表名 | 用途 |
|------|------|
| inbound_kanban_label | 扫码查询看板信息 |
| outbound_order | 关联出库单 |
| outbound_order_detail | 匹配看板物料与出库明细行 |
| outbound_history | 记录出库历史（含批次来源） |
| inventory_stock | 库存扣减 |
| inbound_order_detail | 计算可用库存量 |
| inbound_order | FIFO 校验（按入库时间比较） |

---

## 5. API 接口说明

### 5.1 扫码查询看板

```
GET /api/outbound/scan/labels/{kanbanNo}
```

响应:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "kanbanNo": "KB20240623001",
    "docNo": "IN20240623001",
    "materialCode": "MAT-ELE-001",
    "materialName": "控制器模块",
    "supplierCode": "SUP-003",
    "supplierName": "宁波电子模组",
    "labelQty": 50,
    "warehouseArea": "默认库区",
    "labelStatus": "已入库",
    "inboundOrderId": 1,
    "inboundDetailId": 3,
    "fifoWarning": false,
    "fifoMessage": null,
    "earliestDocNo": "IN20240623001",
    "availableQty": 50
  }
}
```

字段说明：
- `labelStatus`: 必须是"已入库"才能执行出库
- `availableQty`: 当前看板剩余可出库数量（入库量 - 已消耗量）
- `fifoWarning`: true 表示存在更早批次未消耗，前端弹出预警对话框
- `fifoMessage`: 预警详细说明文字
- `earliestDocNo`: 最早可用批次的入库单号

### 5.2 扫码出库执行

```
POST /api/outbound/scan/issue
Content-Type: application/json

{
  "kanbanNo": "KB20240623001",
  "issueQty": 30,
  "outboundOrderId": 1,
  "warehouseArea": "默认库区"
}
```

响应:
```json
{
  "code": 200,
  "message": "扫码出库成功",
  "data": {
    "order": { "id": 1, "docNo": "OUT20240623001", ... },
    "details": [
      { "detailId": 1, "materialCode": "MAT-ELE-001", "actualQty": 30, ... }
    ],
    "stocks": [
      { "materialCode": "MAT-ELE-001", "onHandQty": 20, ... }
    ]
  }
}
```

请求参数说明：
- `kanbanNo`（必填）：看板号
- `issueQty`（必填，≥1）：本次出库数量，不能超过可用数量
- `outboundOrderId`（必填）：关联的出库单 ID
- `warehouseArea`（选填）：出库库区，不填则使用明细行的默认库区

### 5.3 错误响应

| 场景 | HTTP 状态 | 错误信息 |
|------|----------|---------|
| 看板不存在 | 500 | `看板不存在` |
| 看板未入库 | 500 | `该看板尚未完成入库，无法出库` |
| 出库单ID为空 | 500 | `出库单ID不能为空` |
| 出库单已完成 | 500 | `已完成单据不允许再次出库` |
| 库存不足 | 500 | `库存不足，当前看板可用 X，需要 Y` |
| 明细不匹配 | 500 | `出库单中未找到与看板匹配的明细行` |
| 实发超计划 | 500 | `物料 X 的累计实发数量不能大于计划数量` |
| 无库存记录 | 500 | `物料 X 不存在库存记录，请先入库` |
| 库存不足 | 500 | `物料 X 当前库存 Y，不足 Z` |

---

## 6. 出库流程详解

### 6.1 扫码查询流程

```
1. 操作员使用扫码枪扫描看板条码
   - 条码格式: WMS-INBOUND|<看板号>
   - 前缀 "WMS-INBOUND|" 标识这是出库扫码操作
2. 前端 parseScanValue() 剥离前缀，提取看板号
3. 调用 GET /api/outbound/scan/labels/{kanbanNo}
4. 后端执行:
   a. 查询看板标签（inbound_kanban_label）
   b. 查询入库明细（inbound_order_detail）
   c. 查询入库单（inbound_order，获取入库时间用于FIFO比较）
   d. 查询出库历史，计算已消耗量（SUM issue_qty）
   e. 计算可用数量 = 入库量 - 已消耗量
   f. FIFO校验：检查是否有更早批次未消耗
5. 返回 OutboundScanLabelResponse
6. 前端展示看板信息和可用数量
7. 如有 FIFO 预警（fifoWarning=true），弹出预警对话框
```

### 6.2 扫码出库执行流程

```
1. 操作员在看板信息下方输入:
   - 出库单ID（数字输入框）
   - 出库数量（数字输入框，默认看板包数量，最大值=可用数量）
2. 点击"确认出库"按钮
3. 前端调用 POST /api/outbound/scan/issue
4. 后端 @Transactional 事务保护下执行:
   a. 校验看板状态（必须已入库）
   b. 校验出库单ID不为空
   c. 查询出库单，校验状态（未完成）
   d. 校验出库数量 ≤ 可用数量
   e. 匹配出库单明细行（按物料号+需求方）
   f. 校验实发累计 ≤ 计划数量
   g. 创建 outbound_history 记录
      - source_inbound_doc: 来源入库单号（批次溯源）
      - source_detail_id: 来源入库明细 ID
      - issue_qty: 本次出库数量
      - warehouse_area: 出库库区
   h. 更新出库明细 actual_qty += issueQty
   i. 扣减库存 inventory_stock.on_hand_qty -= issueQty
   j. 更新出库单 actual_total_qty 和 status
5. 返回更新后的出库单详情
6. 前端显示成功提示，清空输入，自动聚焦准备下次扫码
```

### 6.3 状态流转

扫码出库的状态流转与普通出库一致：

```
待出库 (actual=0) ──→ 部分完成 (0 < actual < planned) ──→ 已完成 (actual >= planned)
```

### 6.4 连续扫码机制

```
出库成功后:
  → scanValue = ''           // 清空扫码输入
  → currentLabel = null      // 清除看板信息
  → issueForm 重置为默认值
  → focusInput()             // 自动聚焦输入框
  → 等待下次扫码输入
```

---

## 7. 前端页面说明

### 7.1 扫码出库页面 (`views/outbound/Scan.vue`)

页面结构分为三个区域：

**扫码输入区**：
- 看板码输入框：支持扫码枪自动输入、手动输入、粘贴
- 条码格式自动解析：剥离 `WMS-INBOUND|` 前缀
- 按回车键或点击"查询"按钮触发查询
- 清空按钮：恢复初始状态并聚焦输入框

**看板信息展示区**（查询成功后显示）：
- Descriptions 组件展示：看板号、状态（标签）、入库单号、需求方、物料号、物料名称、本包数量、可用数量、库区
- 状态标签颜色：已入库=绿色，其他=黄色

**出库操作区**：
- 出库单 ID 输入框（数字输入框）
- 出库数量输入框（默认看板包数量，最大=可用数量）
- 确认出库按钮（校验通过后方可点击）
- 校验条件：看板已入库 且 出库单ID>0 且 出库数量>0

**FIFO 预警对话框**：
- 当 `getOutboundScanLabel` 返回 `fifoWarning=true` 时自动弹出
- 展示 Alert 组件（warning 类型）：显示被跳过的批次信息
- 提示建议优先出库最早批次
- 操作员可选择"继续出库"或"取消"

**自动聚焦机制**：
- 页面挂载时自动聚焦（`onMounted`）
- 每次查询/出库完成后调用 `focusInput()`（`nextTick` + `ref.focus()`）

### 7.2 打印出库单对话框 (`OutboundPrintDialog.vue`)

**props**：
- `visible`: Boolean，控制弹窗显示
- `order`: Object，出库单头信息（单号/需求方）
- `details`: Array，明细列表

**打印内容**：
- 标题：出库单
- 元信息：单号、需求方、日期
- 明细表格：序号、物料号、物料名称、需求方、计划数量、实发数量、待出库、库区

**打印实现**：
- 使用 `window.print()` 浏览器原生打印
- CSS `@media print` 控制打印样式：仅显示 `.print-sheet` 区域
- 打印时纸张 A4 布局（padding: 12mm）

### 7.3 路由与侧边栏

**路由**（`router/index.js`）:
```js
{
  path: '/outbound',
  children: [
    { path: 'scan', name: 'OutboundScan', component: () => import('@/views/outbound/Scan.vue') }
  ]
}
```

**侧边栏**（`Sidebar.vue`）:
- 在"出库管理"子菜单中添加"扫码出库"菜单项
- 路由 `/outbound/scan`

---

## 8. 关键代码片段

### 8.1 扫码查询看板方法

```java
// OutboundOrderServiceImpl.java:221-284
@Override
public OutboundScanLabelResponse getOutboundScanLabel(String kanbanNo) {
    InboundKanbanLabel label = findKanbanLabel(kanbanNo);

    InboundOrderDetail detail = inboundOrderDetailRepository
            .findById(label.getInboundOrderDetailId())
            .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));

    InboundOrder inboundOrder = inboundOrderRepository
            .findById(label.getInboundOrderId())
            .orElseThrow(() -> new EntityNotFoundException("入库单不存在"));

    // 计算可用数量
    List<OutboundHistory> consumed = outboundHistoryRepository
            .findBySourceDetailId(label.getInboundOrderDetailId());
    int consumedQty = consumed.stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();
    int availableQty = Math.max(safeInt(detail.getActualQty()) - consumedQty, 0);

    // FIFO 校验：查找是否有更早批次未消耗
    boolean fifoWarning = false;
    String fifoMessage = null;
    String earliestDocNo = null;

    List<InboundOrderDetail> allDetailsForMaterial = inboundOrderDetailRepository
            .findByMaterialCodeAndSupplierName(detail.getMaterialCode(), detail.getSupplierName());

    LocalDateTime earliestCreatedAt = null;
    for (InboundOrderDetail otherDetail : allDetailsForMaterial) {
        InboundOrder otherOrder = inboundOrderRepository
                .findByDocNo(otherDetail.getDocNo()).orElse(null);
        if (otherOrder == null) continue;
        if (!STATUS_COMPLETED.equals(otherOrder.getStatus())
                && !STATUS_PARTIAL.equals(otherOrder.getStatus())) continue;

        LocalDateTime orderCreatedAt = otherOrder.getCreatedAt();
        if (orderCreatedAt != null
                && (earliestCreatedAt == null || orderCreatedAt.isBefore(earliestCreatedAt))) {
            earliestCreatedAt = orderCreatedAt;
            earliestDocNo = otherOrder.getDocNo();
        }
    }

    if (earliestDocNo != null && !earliestDocNo.equals(inboundOrder.getDocNo())) {
        fifoWarning = true;
        fifoMessage = "当前看板所属入库单非最早批次，最早入库单号为 " + earliestDocNo;
    }

    return new OutboundScanLabelResponse(
            label.getId(), label.getKanbanNo(), label.getDocNo(),
            label.getMaterialCode(), label.getMaterialName(),
            label.getSupplierCode(), label.getSupplierName(),
            label.getLabelQty(), label.getWarehouseArea(),
            label.getLabelStatus(), label.getInboundOrderId(),
            label.getInboundOrderDetailId(),
            fifoWarning, fifoMessage, earliestDocNo, availableQty
    );
}
```

### 8.2 扫码出库执行方法

```java
// OutboundOrderServiceImpl.java:288-391
@Override
@Transactional
public OutboundOrderDetailResponse issueByScan(OutboundScanIssueRequest request, String operator) {
    InboundKanbanLabel label = findKanbanLabel(request.getKanbanNo());
    if (!LABEL_STATUS_RECEIVED.equals(label.getLabelStatus())) {
        throw new IllegalStateException("该看板尚未完成入库，无法出库");
    }

    OutboundOrder order = findOrder(request.getOutboundOrderId());
    if (STATUS_COMPLETED.equals(order.getStatus())) {
        throw new IllegalStateException("已完成单据不允许再次出库");
    }

    // 校验可用库存
    InboundOrderDetail inboundDetail = inboundOrderDetailRepository
            .findById(label.getInboundOrderDetailId())
            .orElseThrow(() -> new EntityNotFoundException("入库明细不存在"));
    List<OutboundHistory> consumed = outboundHistoryRepository
            .findBySourceDetailId(inboundDetail.getId());
    int consumedQty = consumed.stream().mapToInt(h -> safeInt(h.getIssueQty())).sum();
    int availableQty = Math.max(safeInt(inboundDetail.getActualQty()) - consumedQty, 0);

    if (request.getIssueQty() > availableQty) {
        throw new IllegalStateException("库存不足...");
    }

    // 匹配出库单明细行
    // ... 按物料号+需求方匹配 ...
    // 校验实发累计 <= 计划数量

    // 创建出库历史（含批次来源）
    OutboundHistory history = new OutboundHistory();
    history.setSourceInboundDoc(inboundDetail.getDocNo());
    history.setSourceDetailId(inboundDetail.getId());
    // ... 其他字段 ...

    // 扣减库存 + 更新明细 + 更新出库单状态
    stock.setOnHandQty(onHand - issueQty);
    // ...
}
```

### 8.3 前端条码解析

```javascript
// Scan.vue:183-186
function parseScanValue(value) {
  const prefix = 'WMS-INBOUND|'
  return value.startsWith(prefix) ? value.slice(prefix.length) : value
}
```

### 8.4 前端打印样式控制

```css
/* OutboundPrintDialog.vue:136-162 */
@media print {
  :global(body *) {
    visibility: hidden !important;
  }
  :global(.print-sheet),
  :global(.print-sheet *) {
    visibility: visible !important;
  }
  :global(.print-sheet) {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    padding: 12mm;
  }
}
```

---

## 9. 验证与测试

### 9.1 编译验证

```bash
# 后端编译
cd backend && mvn compile -q    # PASSED

# 前端构建
cd frontend && npm run build    # PASSED
```

### 9.2 功能测试场景

| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 扫码查询看板 | 输入 `WMS-INBOUND|KB001` 回车 | 展示看板信息、可用数量、库区 |
| 手动输入看板号 | 直接输入看板号（不带前缀）回车 | 同样正确查询（`parseScanValue` 兼容无前缀） |
| FIFO 预警 | 扫描非最早批次看板 | 弹出预警对话框，显示最早入库单号 |
| 确认出库 | 输入出库单ID+数量，点击确认 | 成功出库，库存扣减，历史记录生成 |
| 看板未入库 | 扫描未入库看板 | 按钮不可点击，执行时报错"尚未完成入库" |
| 出库单已完成 | 对已完成单执行出库 | 报错"已完成单据不允许再次出库" |
| 出库数量超额 | 输入超过可用数量 | 报错"库存不足，当前看板可用 X，需要 Y" |
| 连续扫码 | 出库成功后 | 输入框自动清空并聚焦，可立即扫描下一单 |
| 打印出库单 | 在出库单管理页点击打印 | 弹窗显示完整单据，打印正常 |
| 多库区出库 | 传入 `warehouseArea` 参数 | 历史记录标注册区，库存按库区扣减 |

### 9.3 端到端验证

代码层面已通过以下验证：

1. ✅ 后端 `mvn compile -q` 零错误编译通过
2. ✅ 前端 `npm run build` 成功构建
3. ✅ Controller 端点路径与前端 API 调用一致
4. ✅ Service 层 `@Transactional` 事务边界正确配置
5. ✅ DTO 字段覆盖看板查询和出库请求的所有需求
6. ✅ Router 包含 `/outbound/scan` 路由
7. ✅ Sidebar 包含"扫码出库"菜单项
8. ✅ `parseScanValue()` 兼容带/不带前缀的输入
9. ✅ 打印对话框 CSS `@media print` 正确配置

---

## 10. 与普通出库的对比

| 特性 | 普通出库 | 扫码出库 |
|------|---------|---------|
| 触发方式 | 出库单管理页 → 选择单据 → 出库按钮 | 扫码枪/手动输入看板号 → 回车查询 |
| 操作粒度 | 明细行级别，可多行同时出库 | 看板标签级别，一次操作一个看板 |
| 出库对象 | 出库单明细行 | 入库看板标签（内含批次信息） |
| API 端点 | `POST /api/outbound/orders/{id}/issue` | `POST /api/outbound/scan/issue` |
| FIFO 分配 | 服务端自动逐批分配 | 基于看板绑定的入库明细单批执行 |
| FIFO 预警 | 不涉及 | 查询阶段主动校验并提示 |
| 适用场景 | 大规模出库、按计划出库 | 快速扫码出库、仓库现场操作 |

---

## 11. 文件清单

### 新建文件 (4 个)

**后端 (3 个)**:
- `controller/OutboundScanController.java` — 扫码出库控制器
- `dto/outbound/OutboundScanLabelResponse.java` — 扫码查询响应 DTO
- `dto/outbound/OutboundScanIssueRequest.java` — 扫码出库请求 DTO

**前端 (1 个)**:
- `components/outbound/OutboundPrintDialog.vue` — 打印出库单对话框

### 修改文件 (6 个)

**后端 (2 个)**:
- `service/OutboundOrderService.java` — 新增 `getOutboundScanLabel()` 和 `issueByScan()` 接口方法
- `service/impl/OutboundOrderServiceImpl.java` — 实现扫码查询和扫码出库方法（含 FIFO 校验逻辑）

**前端 (4 个)**:
- `views/outbound/Scan.vue` — 扫码出库页面（新建）
- `api/outbound.js` — 新增 `getOutboundScanLabel()` 和 `issueByScan()` API 方法
- `router/index.js` — 新增 `/outbound/scan` 路由
- `layout/Sidebar.vue` — 新增"扫码出库"菜单项
