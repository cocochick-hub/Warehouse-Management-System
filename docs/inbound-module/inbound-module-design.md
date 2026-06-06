# 入库模块功能设计文档

## 1. 文档信息

- 模块名称：入库管理
- 负责人：Hang Yang
- 所属项目：Warehouse-Management-System
- 编写日期：2026-06-07
- 版本：v1.0

## 2. 模块目标

入库模块用于支持仓库从创建入库单到执行手工入库的完整业务流程，实现入库单据管理、入库数量登记、状态自动流转以及库存联动更新。

结合当前项目 `README.md` 和现有代码骨架，本模块第一阶段目标如下：

- 实现入库单管理页面
- 实现手工入库页面
- 实现入库单头表与明细表的数据管理
- 实现计划数量与实际数量对比后的状态自动更新
- 为库存管理模块提供入库后的库存增量数据

## 3. 业务范围

### 3.1 包含范围

- 入库单新增
- 入库单列表查询
- 入库单详情查看
- 手工入库登记
- 入库单状态自动流转
- 入库记录与库存变更联动

### 3.2 暂不包含范围

- 条码扫码入库
- PDA 手持设备入库
- 批量 Excel 导入入库单
- 入库审核流
- AI 智能推荐库位

## 4. 角色与权限

结合系统现有角色，建议权限如下：

| 角色 | 权限说明 |
| :--- | :--- |
| admin | 可查看、新增、编辑、执行入库、查看全部数据 |
| manager | 可查看全部入库单，重点查看状态与统计结果 |
| operator | 可创建入库单、执行手工入库、查看本人操作数据 |

## 5. 业务流程设计

### 5.1 入库单创建流程

1. 操作员进入“入库单管理”页面。
2. 点击“创建入库单”。
3. 选择供应商。
4. 添加一个或多个零件明细。
5. 填写计划入库数量、包装容量等信息。
6. 保存后系统生成唯一入库单号。
7. 系统默认单据状态为“未入库”。

### 5.2 手工入库流程

1. 操作员进入“手工入库”页面。
2. 选择待处理的入库单。
3. 打开单据明细，录入每个零件的实际入库数量。
4. 系统校验实际入库数量不能小于 0。
5. 系统汇总实际数量并与计划数量进行比较。
6. 系统更新单据状态：
   - 实际数量全部为 0：未入库
   - 实际数量大于 0 且小于计划数量：部分完成
   - 实际数量等于计划数量：已完成
7. 系统同步增加库存数量。

### 5.3 状态流转规则

| 当前状态 | 触发条件 | 下一状态 |
| :--- | :--- | :--- |
| 未入库 | 任一明细实际入库数量大于 0，且未全部完成 | 部分完成 |
| 未入库 | 所有明细实际入库数量均达到计划数量 | 已完成 |
| 部分完成 | 所有明细实际入库数量均达到计划数量 | 已完成 |

说明：

- 状态由系统自动计算，不允许前端手工选择。
- 第一阶段状态字段统一直接使用中文值：`未入库`、`部分完成`、`已完成`。
- 若后续支持撤销入库，需要增加反向库存回滚逻辑。

## 6. 功能设计

### 6.1 入库单管理页面

页面建议对应前端文件：

- `frontend/src/views/inbound/Order.vue`

功能点：

- 条件查询
- 入库单列表展示
- 新建入库单
- 查看入库单详情
- 跳转手工入库

查询条件建议：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| 单号 | 输入框 | 支持模糊查询 |
| 供应商 | 下拉框 | 选择供应商 |
| 状态 | 下拉框 | 未入库、部分完成、已完成 |
| 创建日期 | 日期范围 | 便于按时间筛选 |

列表字段建议：

| 字段 | 说明 |
| :--- | :--- |
| 单号 | 系统生成，唯一 |
| 供应商 | 供应商名称 |
| 零件种类数 | 根据明细聚合 |
| 计划总数 | 明细计划数量汇总 |
| 实收总数 | 明细实收数量汇总 |
| 状态 | 自动计算 |
| 创建时间 | 审计字段 |

### 6.2 创建入库单弹窗/页面

建议字段：

| 字段 | 是否必填 | 说明 |
| :--- | :--- | :--- |
| 供应商 | 是 | 从供应商主数据选择 |
| 零件号 | 是 | 从物料主数据选择 |
| 零件名称 | 否 | 自动带出 |
| 包装容量 | 否 | 可根据包装信息带出 |
| 计划入库数量 | 是 | 大于 0 的整数 |
| 备注 | 否 | 预留说明 |

交互要求：

- 支持动态增删明细行
- 至少保留一条明细
- 保存前校验必填项和数量合法性

### 6.3 手工入库页面

页面建议对应前端文件：

- `frontend/src/views/inbound/Manual.vue`

功能点：

- 查询待处理入库单
- 选择单据查看明细
- 录入实际入库数量
- 提交后更新状态和库存

列表字段建议：

| 字段 | 说明 |
| :--- | :--- |
| 入库单号 | 单据编号 |
| 供应商 | 供应商名称 |
| 状态 | 当前状态 |
| 操作 | 开始入库 |

入库明细弹窗建议：

| 字段 | 说明 |
| :--- | :--- |
| 零件号 | 只读 |
| 零件名称 | 只读 |
| 计划入库数量 | 只读 |
| 已入库数量 | 只读 |
| 本次入库数量 | 可编辑 |
| 入库后累计数量 | 自动计算 |

### 6.4 后端接口设计

建议新增控制器：

- `InboundOrderController`

建议接口如下：

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | `/api/inbound/orders` | 分页查询入库单 |
| GET | `/api/inbound/orders/{id}` | 查询入库单详情 |
| POST | `/api/inbound/orders` | 新增入库单 |
| PUT | `/api/inbound/orders/{id}` | 编辑未完成入库单 |
| POST | `/api/inbound/orders/{id}/receive` | 执行手工入库 |

接口返回统一使用现有 `ApiResult`。

## 7. 数据库设计

当前仓库已有 `inbound_order` 表，但仅能支持单头信息，不足以支撑“创建入库单 + 多明细 + 手工入库 + 状态更新 + 最小库存联动”闭环。结合当前代码骨架与 `README.md` 的演示目标，本轮数据库设计定稿如下。

### 7.1 定稿原则

- 以“最小可演示闭环”优先，不为未来全量 WMS 一次性过度建模
- 单据头表保存汇总字段，减少列表查询与状态计算复杂度
- 单据明细表保存业务快照字段，降低当前对物料主数据、供应商主数据的依赖
- 库存联动采用“最小库存快照表”方案，先满足入库后库存可验证变化
- 当前未落地的主数据模块先不强行建立外键依赖，避免 Card 04 和后续联调被基础模块阻塞

### 7.2 入库单头表 `inbound_order`

最终建议字段：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | BIGINT | 主键 |
| doc_no | VARCHAR(50) | 入库单号，唯一 |
| supplier | VARCHAR(100) | 供应商名称快照 |
| status | VARCHAR(20) | 单据状态：未入库/部分完成/已完成 |
| item_count | INT | 明细条数汇总 |
| planned_total_qty | INT | 计划总数汇总 |
| actual_total_qty | INT | 实收总数汇总 |
| remark | VARCHAR(255) | 备注 |
| created_by | VARCHAR(50) | 创建人 |
| updated_by | VARCHAR(50) | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

设计说明：

- `item_count`、`planned_total_qty`、`actual_total_qty` 由后端在创建单据和执行入库时维护
- `status` 不允许前端直接编辑，统一由后端根据明细累计实收数量自动计算
- 第一阶段保留 `supplier` 文本快照，不强依赖供应商主数据表

建议索引：

- 唯一索引：`uk_inbound_order_doc_no (doc_no)`
- 普通索引：`idx_inbound_order_status (status)`
- 普通索引：`idx_inbound_order_supplier (supplier)`
- 普通索引：`idx_inbound_order_created_at (created_at)`

### 7.3 入库单明细表 `inbound_order_detail`

本表为 Card 03 新增的核心表。

最终建议字段：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | BIGINT | 主键 |
| inbound_order_id | BIGINT | 入库单头主键 |
| doc_no | VARCHAR(50) | 冗余单号，便于按单号联查 |
| line_no | INT | 行号，便于前端展示和排序 |
| material_code | VARCHAR(50) | 零件号/物料号 |
| material_name | VARCHAR(100) | 零件名称快照 |
| packaging_capacity | INT | 包装容量 |
| planned_qty | INT | 计划入库数量 |
| actual_qty | INT | 累计实际入库数量 |
| remark | VARCHAR(255) | 明细备注 |
| created_by | VARCHAR(50) | 创建人 |
| updated_by | VARCHAR(50) | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

设计说明：

- 一张入库单可有多条明细，这是本轮闭环的必需结构
- `actual_qty` 保存累计实收数量，不单独为每次提交再建流水表，先满足第一阶段演示
- `line_no` 用于保持录入顺序，避免前端仅靠主键排序
- 同一张单据中默认不允许重复 `material_code`
- 第一阶段禁止超收，因此必须满足 `0 <= actual_qty <= planned_qty`

建议索引与约束：

- 外键：`fk_inbound_order_detail_order_id (inbound_order_id -> inbound_order.id)`
- 普通索引：`idx_inbound_order_detail_doc_no (doc_no)`
- 普通索引：`idx_inbound_order_detail_material_code (material_code)`
- 唯一索引：`uk_inbound_order_detail_order_material (inbound_order_id, material_code)`

### 7.4 最小库存快照表 `inventory_stock`

当前仓库中尚无可直接复用的库存实体和库存表实现。为了满足“入库后库存变化可验证”的演示要求，本轮建议增加最小库存快照表，而不是等待完整库存模块落地。

最终建议字段：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | BIGINT | 主键 |
| material_code | VARCHAR(50) | 物料号 |
| material_name | VARCHAR(100) | 物料名称快照 |
| supplier | VARCHAR(100) | 供应商名称快照 |
| on_hand_qty | INT | 当前库存数量 |
| last_inbound_doc_no | VARCHAR(50) | 最近一次入库单号 |
| last_inbound_at | DATETIME | 最近一次入库时间 |
| created_by | VARCHAR(50) | 创建人 |
| updated_by | VARCHAR(50) | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

设计说明：

- 本表只承担第一阶段“入库库存联动结果展示”，不承诺覆盖完整库存管理需求
- 库存粒度先按 `material_code + supplier` 维护，避免不同供应来源混淆
- 高低储天数、安全库存、批次、库位等字段留待库存模块正式建设时补充

建议索引与约束：

- 唯一索引：`uk_inventory_stock_material_supplier (material_code, supplier)`
- 普通索引：`idx_inventory_stock_last_inbound_at (last_inbound_at)`

### 7.5 状态计算与字段维护规则

单据头表汇总字段应以明细数据为准，由后端统一维护：

- `item_count = 明细行数`
- `planned_total_qty = sum(detail.planned_qty)`
- `actual_total_qty = sum(detail.actual_qty)`

单据状态计算规则定稿如下：

| 条件 | 状态 |
| :--- | :--- |
| `actual_total_qty = 0` | 未入库 |
| `0 < actual_total_qty < planned_total_qty` | 部分完成 |
| `actual_total_qty = planned_total_qty` | 已完成 |

补充约束：

- 任一明细 `actual_qty` 不得大于 `planned_qty`
- 若存在任一明细 `actual_qty > 0` 且未全部达到计划数，则头表状态必须为“部分完成”
- 已完成单据不允许再次执行入库

### 7.6 Card 04 推荐 SQL 落地方案

考虑到当前 `schema.sql` 仍有编码和初始化风险，Card 04 建议采用“入库相关表局部重建”的方式，而不是对现有 `inbound_order` 做多次 `ALTER TABLE` 累积修改。

推荐顺序：

1. `DROP TABLE IF EXISTS inbound_order_detail`
2. `DROP TABLE IF EXISTS inbound_order`
3. `DROP TABLE IF EXISTS inventory_stock`
4. 重建 `inbound_order`
5. 重建 `inbound_order_detail`
6. 重建 `inventory_stock`
7. 插入最小演示数据

推荐 SQL 草案如下：

```sql
DROP TABLE IF EXISTS inbound_order_detail;
DROP TABLE IF EXISTS inbound_order;
DROP TABLE IF EXISTS inventory_stock;

CREATE TABLE inbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    doc_no            VARCHAR(50)  NOT NULL COMMENT '入库单号',
    supplier          VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    status            VARCHAR(20)  NOT NULL DEFAULT '未入库' COMMENT '状态：未入库/部分完成/已完成',
    item_count        INT          NOT NULL DEFAULT 0 COMMENT '明细条数',
    planned_total_qty INT          NOT NULL DEFAULT 0 COMMENT '计划总数',
    actual_total_qty  INT          NOT NULL DEFAULT 0 COMMENT '实收总数',
    remark            VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_by        VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by        VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_inbound_order_doc_no (doc_no),
    KEY idx_inbound_order_status (status),
    KEY idx_inbound_order_supplier (supplier),
    KEY idx_inbound_order_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单头表';

CREATE TABLE inbound_order_detail (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    inbound_order_id   BIGINT       NOT NULL COMMENT '入库单ID',
    doc_no             VARCHAR(50)  NOT NULL COMMENT '入库单号冗余',
    line_no            INT          NOT NULL COMMENT '行号',
    material_code      VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name      VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    packaging_capacity INT          DEFAULT NULL COMMENT '包装容量',
    planned_qty        INT          NOT NULL COMMENT '计划入库数量',
    actual_qty         INT          NOT NULL DEFAULT 0 COMMENT '累计实际入库数量',
    remark             VARCHAR(255) DEFAULT NULL COMMENT '明细备注',
    created_by         VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by         VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_inbound_order_detail_order_id FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    UNIQUE KEY uk_inbound_order_detail_order_material (inbound_order_id, material_code),
    KEY idx_inbound_order_detail_doc_no (doc_no),
    KEY idx_inbound_order_detail_material_code (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

CREATE TABLE inventory_stock (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_code       VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name       VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    supplier            VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    on_hand_qty         INT          NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    last_inbound_doc_no VARCHAR(50)  DEFAULT NULL COMMENT '最近入库单号',
    last_inbound_at     DATETIME     DEFAULT NULL COMMENT '最近入库时间',
    created_by          VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by          VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_inventory_stock_material_supplier (material_code, supplier),
    KEY idx_inventory_stock_last_inbound_at (last_inbound_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最小库存快照表';
```

## 8. 核心类设计建议

### 8.1 后端分层建议

建议新增或扩展以下类：

- `entity/InboundOrder.java`
- `entity/InboundOrderDetail.java`
- `repository/InboundOrderRepository.java`
- `repository/InboundOrderDetailRepository.java`
- `service/InboundOrderService.java`
- `service/impl/InboundOrderServiceImpl.java`
- `controller/InboundOrderController.java`
- `dto/inbound/InboundOrderCreateRequest.java`
- `dto/inbound/InboundReceiveRequest.java`
- `dto/inbound/InboundOrderDetailDTO.java`

### 8.2 前端结构建议

建议新增或扩展以下文件：

- `frontend/src/api/inbound.js`
- `frontend/src/views/inbound/Order.vue`
- `frontend/src/views/inbound/Manual.vue`
- `frontend/src/components/inbound/InboundOrderForm.vue`
- `frontend/src/components/inbound/InboundReceiveDialog.vue`

## 9. 校验规则

### 9.1 前端校验

- 供应商不能为空
- 明细不能为空
- 计划入库数量必须大于 0
- 实际入库数量必须大于等于 0
- 同一张单据中不允许重复选择同一物料

### 9.2 后端校验

- 入库单号唯一
- 入库单至少包含一条明细
- 已完成单据不允许再次编辑计划数量
- 实际入库数量累计不能无限制重复增加，需结合业务判断是否允许超收

建议第一阶段先做“禁止超收”，即：

- 明细累计实际数量不能大于计划数量

## 10. 异常与边界场景

| 场景 | 处理建议 |
| :--- | :--- |
| 保存单据时供应商不存在 | 拒绝保存并提示 |
| 明细数量为 0 或负数 | 拒绝提交 |
| 入库时单据不存在 | 返回 404 或业务错误 |
| 重复提交入库 | 后端做幂等和状态校验 |
| 已完成单据继续入库 | 拒绝并提示 |
| 实际数量大于计划数量 | 第一阶段禁止超收 |

## 11. 非功能要求

- 列表查询响应时间尽量控制在 2 秒内
- 单据与明细保存要保证事务一致性
- 入库执行与库存更新需放在同一事务中
- 页面表单需要具备基本易用性与错误提示

## 12. 第一阶段交付物

- 入库单管理页面可新增、查询、查看
- 手工入库页面可对单据执行入库
- 后端完成入库单和明细接口
- 数据库补充入库明细表
- 状态自动流转和库存更新逻辑可运行

## 13. 风险与待确认项

- 当前 `schema.sql` 存在乱码和结构简化问题，Card 04 落地时应优先局部重建入库相关表，避免全量脚本大改
- 当前系统中物料、供应商、库存实体尚未完整落地，因此本轮采用“单据快照字段 + 最小库存快照表”方案
- 是否允许超收、部分收货多次提交，需要尽早和老师或组长确认业务规则；本轮默认禁止超收、允许分多次提交直至完成
