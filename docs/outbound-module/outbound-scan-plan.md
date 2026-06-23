# 出库扫码模块 - 实施计划

## 计划概览

本计划将出库扫码模块的实现分解为 3 个阶段，共 12 个子任务。遵循"后端先行、前端跟随"的策略，复用已有出库单管理模块的数据层。

## 阶段一：后端 — 控制器层 + DTO

### 任务 1.1：创建 OutboundScanDTO

- **文件**: `backend/src/main/java/com/example/wms/dto/outbound/OutboundScanDTO.java`
- **操作**: 新建 DTO，封装扫码查询出库单的响应
- **字段**:
  - `OutboundOrderSummaryDTO order` — 出库单头信息
  - `List<OutboundOrderDetailDTO> details` — 明细列表
  - `List<InventoryStockDTO> stocks` — 关联库存快照
- **验证**: DTO 编译通过，字段覆盖需求

### 任务 1.2：创建 OutboundScanIssueRequest

- **文件**: `backend/src/main/java/com/example/wms/dto/outbound/OutboundScanIssueRequest.java`
- **操作**: 新建扫码出库请求 DTO
- **字段**:
  - `@NotBlank String docNo` — 出库单号
  - `List<OutboundScanIssueDetail> details` — 出库明细
- **嵌套类 `OutboundScanIssueDetail`**:
  - `@NotNull Long detailId` — 明细ID
  - `@Min(1) Integer issueQty` — 本次出库数量
  - `String warehouseArea` — 出库库区（默认与明细行一致）
- **验证**: 校验注解正确，编译通过

### 任务 1.3：创建 OutboundScanIssueResponse

- **文件**: `backend/src/main/java/com/example/wms/dto/outbound/OutboundScanIssueResponse.java`
- **操作**: 新建扫码出库响应 DTO
- **字段**:
  - `OutboundOrderSummaryDTO order` — 出库单信息
  - `List<OutboundOrderDetailDTO> details` — 更新后的明细
  - `List<FifoWarning> fifoWarnings` — FIFO 校验警告
  - `List<Long> historyIds` — 生成的出库历史ID
- **嵌套类 `FifoWarning`**:
  - `String materialCode` — 物料号
  - `String materialName` — 物料名称
  - `String skippedBatch` — 被跳过的入库单号
  - `LocalDateTime skippedInboundTime` — 被跳过批次的入库时间
- **验证**: 编译通过

### 任务 1.4：扩展 OutboundOrderController — 扫码接口

- **文件**: `backend/src/main/java/com/example/wms/controller/OutboundOrderController.java`
- **操作**: 在现有控制器中添加扫码查询和扫码出库端点（不新建独立 Controller）
- **新增端点**:
  - `GET /api/outbound/scan/{docNo}` — 扫码查询出库单
  - `POST /api/outbound/scan/issue` — 扫码出库执行
- **验证**: 端点可访问，参数绑定正确

## 阶段二：后端 — 服务层

### 任务 2.1：扩展 OutboundOrderService 接口

- **文件**: `backend/src/main/java/com/example/wms/service/OutboundOrderService.java`
- **新增方法**:
  - `OutboundScanDTO getScanOrder(String docNo)` — 扫码查询出库单（含库存快照）
  - `OutboundScanIssueResponse issueByScan(OutboundScanIssueRequest request, String operator)` — 扫码出库执行
  - `List<FifoWarning> checkFifoOrder(Long orderId, List<AllocatedBatchRecord> allocations)` — FIFO 校验
- **验证**: 接口方法签名完整

### 任务 2.2：实现 OutboundOrderServiceImpl — 扫码查询

- **文件**: `backend/src/main/java/com/example/wms/service/impl/OutboundOrderServiceImpl.java`
- **操作**: 实现 `getScanOrder(docNo)` 方法
- **核心逻辑**:
  1. 根据 `docNo` 查询 `OutboundOrder`
  2. 查询关联的 `OutboundOrderDetail` 列表
  3. 查询各明细对应的 `InventoryStock`（按物料+供应商）
  4. 组装 `OutboundScanDTO` 返回
- **异常**:
  - 出库单不存在 → 抛 `BusinessException("出库单不存在")`
  - 出库单已完成 → 抛 `BusinessException("出库单已完成，无法再出库")`

### 任务 2.3：实现 OutboundOrderServiceImpl — 扫码出库（含 FIFO 校验）

- **文件**: `backend/src/main/java/com/example/wms/service/impl/OutboundOrderServiceImpl.java`
- **操作**: 实现 `issueByScan(request, operator)` 方法
- **核心逻辑**:
  1. 校验出库单状态（仅待出库/部分完成可执行）
  2. 遍历每个出库明细，执行 FIFO 分配：
     - 查询该物料+供应商的所有已入库明细，按入库时间升序
     - 消耗未完全扣减的入库批次
     - 记录分配结果 `(inboundDetailId, sourceDocNo, allocatedQty)`
  3. 执行 FIFO 校验预警 `checkFifoOrder`：
     - 检查分配的最早批次前是否有更早批次仍有剩余库存
     - 生成 `FifoWarning` 列表
  4. 库存扣减：更新 `inventory_stock.on_hand_qty`
  5. 更新出库明细 `actual_qty`
  6. 写入 `outbound_history`（含库区、来源批次）
  7. 更新出库单状态（`actual_total_qty`，全部完成则状态改为"已完成"）
  8. 组装 `OutboundScanIssueResponse` 返回
- **事务**: 整个方法标注 `@Transactional`
- **多库区**: 明细级 `warehouse_area` 参数传递到 FIFO 分配和库存扣减，不同库区独立扣减

### 任务 2.4：实现 FIFO 校验方法

- **文件**: `backend/src/main/java/com/example/wms/service/impl/OutboundOrderServiceImpl.java`
- **操作**: 实现 `checkFifoOrder(orderId, allocations)` 私有方法
- **核心逻辑**:
  1. 获取本次分配中涉及的最早入库批次时间 `earliestAllocatedTime`
  2. 查询所有早于 `earliestAllocatedTime` 的入库明细
  3. 过滤出仍有剩余库存的批次（`actualQty - consumedQty > 0`）
  4. 生成 `FifoWarning` 列表
- **已消耗量**: `outbound_history` 中 `source_detail_id` GROUP BY 聚合
- **验证**: 有跳批场景产生警告，无跳批场景返回空列表

## 阶段三：前端实现

### 任务 3.1：扩展前端 API 封装

- **文件**: `frontend/src/api/outbound.js`
- **新增方法**:
  - `getScanOrder(docNo)` — `GET /api/outbound/scan/{docNo}`
  - `issueByScan(data)` — `POST /api/outbound/scan/issue`
- **验证**: API 调用返回正确数据结构

### 任务 3.2：实现扫码出库页

- **文件**: `frontend/src/views/outbound/Scan.vue`
- **功能**:
  - 输入框 + 扫码/回车查询（自动解析 `WMS-OUTBOUND|` 前缀）
  - 出库单信息展示区（Descriptions 组件）
  - 明细表格（el-table）：物料号、物料名称、计划数、已出库数、本次出库数（el-input-number）、库区（el-select）
  - FIFO 警告区域（el-alert type="warning" 动态渲染）
  - 确认出库按钮（校验明细、数量不超过剩余计划数）
  - 清空按钮（恢复初始状态，聚焦输入框）
  - 自动聚焦机制（`nextTick` + `focus()`）
- **参考**: `frontend/src/views/inbound/Scan.vue` 的交互模式
- **验证**: 页面渲染正常，交互流畅

### 任务 3.3：实现打印出库单对话框

- **文件**: `frontend/src/components/outbound/OutboundPrintDialog.vue`
- **功能**:
  - 通过 props 接收出库单数据和历史记录
  - 弹窗展示 A4 布局的出库单据
  - 内容区：出库单号、需求方、日期、明细表格（物料/批量/数量）
  - 操作人、时间戳
  - 二维码（`qrcode` 库生成，内容指向出库单详情）
  - 打印按钮（`window.print()`）
- **样式**: 参考 `InboundKanbanPrintDialog.vue` 的打印样式方案
- **验证**: 弹窗展示，打印功能正常

### 任务 3.4：更新路由和侧边栏

- **文件**: `frontend/src/router/index.js`
- **操作**: 在 `outbound` 路由组中添加扫码路由：
  ```js
  {
    path: 'scan',
    name: 'OutboundScan',
    component: () => import('@/views/outbound/Scan.vue'),
    meta: { title: '扫码出库' }
  }
  ```
- **文件**: `frontend/src/layout/Sidebar.vue`
- **操作**: 在"出库管理"子菜单中添加"扫码出库"菜单项：
  ```html
  <el-menu-item index="/outbound/scan">
    <el-icon><Aim /></el-icon>
    <span>扫码出库</span>
  </el-menu-item>
  ```
- **验证**: 菜单可点击，路由跳转正确

## 任务依赖关系

```
阶段一（控制器 + DTO）
  ├── 1.1 OutboundScanDTO
  ├── 1.2 OutboundScanIssueRequest
  ├── 1.3 OutboundScanIssueResponse
  └── 1.4 Controller 扩展 ──────── 依赖 1.1 + 1.2 + 1.3
       │
       ▼
阶段二（服务层）
  ├── 2.1 Service 接口
  ├── 2.2 扫码查询实现
  ├── 2.3 扫码出库实现
  └── 2.4 FIFO 校验实现 ───────── 依赖 2.3
       │
       ▼
阶段三（前端）
  ├── 3.1 API 封装
  ├── 3.2 扫码出库页
  ├── 3.3 打印出库单对话框 ─────── 独立于 3.2
  └── 3.4 路由和侧边栏 ────────── 依赖 3.2
```

同级任务可并行执行。

## 关键设计决策

1. **不复用 Issue 接口**：扫码出库使用独立的 `POST /api/outbound/scan/issue` 接口，不同于现有的 `POST /api/outbound/orders/{id}/issue`。原因：扫码接口支持直接通过单号执行、返回 FIFO 警告、一次调用完成全部明细出库。

2. **Controller 扩展方式**：不新建独立的 `OutboundScanController`，而是在现有 `OutboundOrderController` 中添加扫码端点。原因：扫码出库是出库管理模块的子功能，共用同一 Service。

3. **FIFO 校验不阻断出库**：FIFO 预警作为信息提示返回前端，不抛异常阻断出库流程。原因：实际仓库场景中可能存在合法的非 FIFO 出库需求（如紧急出库、批次特批）。

4. **多库区通过明细级参数支持**：在 `OutboundScanIssueDetail` 中添加 `warehouseArea` 字段，允许同一条出库明细从不同库区扣减。FIFO 分配按 `(物料, 供应商, 库区)` 三元组独立执行。

5. **打印方案**：复用浏览器 `window.print()` 原生打印，配合 CSS `@media print` 控制打印样式，对齐 `InboundKanbanPrintDialog` 的实现方案。

6. **条码格式**：统一使用 `WMS-OUTBOUND|<出库单号>` 前缀格式，与入库 `WMS-INBOUND|` 保持命名一致性。
