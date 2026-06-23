# 出库管理模块 - 实施计划

## 计划概览

本计划将出库管理模块的实现分解为 4 个阶段，共 16 个子任务。遵循"后端先行、前端跟随、集成验证"的策略。

## 阶段一：数据层（数据库 + Entity + Repository）

### 任务 1.1：更新数据库表结构
- **文件**: `backend/src/main/resources/schema.sql`
- **操作**: 
  - 扩展 `outbound_order` 表，添加 `item_count`, `planned_total_qty`, `actual_total_qty`, `remark` 字段
  - 创建 `outbound_order_detail` 表
  - 创建 `outbound_history` 表
  - 添加演示数据
- **验证**: DDL 语法正确，表结构完整

### 任务 1.2：创建 OutboundOrderDetail 实体
- **文件**: `backend/src/main/java/com/example/wms/entity/OutboundOrderDetail.java`
- **操作**: 新建 JPA 实体类，映射 `outbound_order_detail` 表
- **字段**: id, outboundOrderId, docNo, lineNo, supplierCode, supplierName, materialCode, materialName, plannedQty, actualQty, warehouseArea, remark + BaseEntity

### 任务 1.3：创建 OutboundHistory 实体
- **文件**: `backend/src/main/java/com/example/wms/entity/OutboundHistory.java`
- **操作**: 新建 JPA 实体类，映射 `outbound_history` 表
- **字段**: id, outboundOrderId, outboundDetailId, docNo, materialCode, materialName, supplierName, issueQty, sourceInboundDoc, sourceDetailId, warehouseArea, issuedBy + createdAt

### 任务 1.4：扩展 OutboundOrder 实体
- **文件**: `backend/src/main/java/com/example/wms/entity/OutboundOrder.java`
- **操作**: 添加 itemCount, plannedTotalQty, actualTotalQty, remark 字段

### 任务 1.5：创建/扩展 Repository
- **文件**: 
  - 新建 `OutboundOrderDetailRepository.java`
  - 新建 `OutboundHistoryRepository.java`
  - 扩展 `OutboundOrderRepository.java`（添加分页查询方法）
  - 扩展 `InboundOrderDetailRepository.java`（添加 FIFO 查询方法）

## 阶段二：服务层（DTO + Service）

### 任务 2.1：创建出库 DTO 类
- **目录**: `backend/src/main/java/com/example/wms/dto/outbound/`
- **文件**:
  - `OutboundOrderSummaryDTO.java` - 列表摘要
  - `OutboundOrderDetailDTO.java` - 明细行
  - `OutboundOrderCreateRequest.java` - 创建请求
  - `OutboundCreateDetailRequest.java` - 创建明细请求
  - `OutboundIssueRequest.java` - 出库请求
  - `OutboundIssueDetailRequest.java` - 出库明细请求
  - `OutboundOrderPageResponse.java` - 分页响应
  - `OutboundOrderDetailResponse.java` - 详情响应
  - `OutboundHistoryDTO.java` - 历史记录

### 任务 2.2：创建 OutboundOrderService 接口
- **文件**: `backend/src/main/java/com/example/wms/service/OutboundOrderService.java`
- **方法**:
  - `listOrders(...)` - 分页查询
  - `getOrderDetail(id)` - 详情
  - `createOrder(request, operator)` - 创建
  - `issueOrder(id, request, operator)` - 出库执行（FIFO）
  - `listHistory(docNo, page, size)` - 出库历史

### 任务 2.3：实现 OutboundOrderServiceImpl
- **文件**: `backend/src/main/java/com/example/wms/service/impl/OutboundOrderServiceImpl.java`
- **核心逻辑**:
  1. `createOrder`: 参考 inbound 模式，自动生成 OUT 前缀单号，校验重复物料，计算汇总字段
  2. `issueOrder`: FIFO 分配 + 库存扣减 + 历史记录 + 状态更新（事务保护）
  3. `allocateFIFO`: 按入库时间升序消耗库存批次
  4. `deductInventory`: 扣减 `inventory_stock` 表库存量
  5. `recordHistory`: 写入 `outbound_history` 表
  6. `listHistory`: 分页查询历史记录

## 阶段三：控制器层 + 仪表盘集成

### 任务 3.1：创建 OutboundOrderController
- **文件**: `backend/src/main/java/com/example/wms/controller/OutboundOrderController.java`
- **端点**:
  - `GET /api/outbound/orders` - 列表
  - `GET /api/outbound/orders/{id}` - 详情
  - `POST /api/outbound/orders` - 创建
  - `POST /api/outbound/orders/{id}/issue` - 出库
  - `GET /api/outbound/history` - 历史

### 任务 3.2：更新仪表盘 API
- **文件**: `backend/src/main/java/com/example/wms/service/impl/DashboardServiceImpl.java`
- **操作**: 
  - `pendingOutbound` 使用 `outboundOrderRepository.countByStatusNot("已完成")`
  - `lowStockAlert` 使用真实值: `inventoryStockRepository.countByOnHandQty(0)`
  - `totalMaterials` 使用 `materialInfoRepository.count()`
  - 添加 `totalSuppliers`, `todayOutbound` 统计
- **文件**: `backend/src/main/java/com/example/wms/dto/DashboardDTO.java`
- **操作**: 扩展 DTO 添加新字段

## 阶段四：前端实现

### 任务 4.1：创建 API 封装
- **文件**: `frontend/src/api/outbound.js`
- **方法**: listOrders, getOrderDetail, createOrder, issueOrder, listHistory

### 任务 4.2：实现出库单管理页
- **文件**: `frontend/src/views/outbound/Order.vue`
- **功能**: 
  - 搜索栏（单号/需求方/状态筛选）
  - 分页表格
  - "创建出库单"按钮 + 对话框
  - "执行出库"按钮 + 对话框
  - "查看详情"按钮 + 对话框

### 任务 4.3：实现出库组件
- **文件**: 
  - `frontend/src/components/outbound/OutboundOrderForm.vue` - 创建表单
  - `frontend/src/components/outbound/OutboundIssueDialog.vue` - 出库执行
  - `frontend/src/components/outbound/OutboundOrderDetailDialog.vue` - 详情查看

### 任务 4.4：实现出库历史页
- **文件**: `frontend/src/views/outbound/History.vue`
- **功能**: 按出库单号查询历史记录，显示批次溯源信息

### 任务 4.5：更新仪表盘
- **文件**: `frontend/src/views/Dashboard.vue`
- **操作**: 
  - 统计卡片从 API 获取数据（不再硬编码）
  - 添加供应商总数和今日出库数卡片

### 任务 4.6：更新路由和侧边栏
- **文件**: 
  - `frontend/src/router/index.js` - 添加 outbound/history 路由
  - `frontend/src/layout/Sidebar.vue` - 添加"出库历史"菜单项

## 任务依赖关系

```
阶段一（数据层）
  ├── 1.1 schema.sql ──── 1.2 OutboundOrderDetail 实体
  ├── 1.4 OutboundOrder 扩展    1.3 OutboundHistory 实体
  └── 1.5 Repository 层
       │
       ▼
阶段二（服务层）
  ├── 2.1 DTO ────── 2.2 Service 接口 ────── 2.3 Service 实现
       │
       ▼
阶段三（控制器 + 仪表盘）
  ├── 3.1 OutboundOrderController ────── 3.2 Dashboard 更新
       │
       ▼
阶段四（前端）
  ├── 4.1 API 封装
  ├── 4.2 出库单管理页 ──── 4.3 出库组件
  ├── 4.4 出库历史页
  ├── 4.5 仪表盘更新
  └── 4.6 路由和侧边栏
```

同级任务可并行执行。

## 关键设计决策

1. **FIFO 实现方式**: 不新增库存批次表，直接通过 `inbound_order_detail.actual_qty` 扣除 `outbound_history` 累计消耗量计算可用库存。查询已耗用量通过 `outbound_history.source_detail_id` GROUP BY 聚合。
2. **出库单号格式**: `OUT{yyyyMMddHHmmssSSS}{3位序号}`，参考入库单 IN 前缀模式。
3. **出库历史表**: 每次出库操作创建一批历史记录，每条记录关联一个 FIFO 批次来源。
4. **事务边界**: `issueOrder` 方法整体加 `@Transactional`，确保出库数量更新、库存扣减、历史记录写入在同一事务中。
