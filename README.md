# 汽车物流WMS仓库管理系统

> 面向汽车物流行业的仓储管理系统，涵盖入库、出库、退库、转包、封存、库存监控等核心业务，支持 AI 智能分析与移动端操作。

## 项目概述

本项目是一个面向汽车物流行业的仓储管理系统开发实训项目，涵盖基础信息管理、入库管理、出库管理、退库管理、转包管理、封存管理、库存监控、高低储预警、物料需求管理等核心业务模块，采用前后端分离架构。

## 技术栈

### 前端
- Vue 3
- Element Plus
- Axios（HTTP 请求）
- Vue Router（路由管理）
- Pinia（状态管理）
- Vite（构建工具）

### 后端
- Java JDK 17+
- Spring Boot 2.7.x
- Spring Security（认证与授权）
- Spring Data JPA（数据访问）
- MySQL 5.7+
- JWT（jjwt 0.9.1，无状态认证）
- BCrypt（密码加密）

### 移动端
- uni-app（Vue 3 模式）
- Vant 4（移动端 UI）
- Pinia（状态管理）
- HBuilder X（云打包 APK）

## 功能模块

### 0. 系统登录与用户认证

提供用户登录、Token 续期和权限校验功能。

| 功能 | 接口 | 说明 |
| :--- | :--- | :--- |
| **登录** | POST /api/auth/login | 用户名+密码登录，返回 JWT Token |
| **Token 刷新** | POST /api/auth/refresh | 过期 7 天内可无感刷新 |
| **获取用户信息** | GET /api/auth/userInfo | 根据 Token 获取当前用户信息 |

**用户角色体系：**
- `admin` — 系统管理员，拥有全部权限
- `manager` — 仓库经理，审核与报表查看
- `operator` — 操作员，日常出入库操作

### 1. 基础信息管理

维护系统运行所需的静态主数据。

| 页面 | 核心字段 | 说明 |
| :--- | :--- | :--- |
| **物料管理** | 物料号、物料名称、类型、单位、供应商 | CRUD + 分页 + 模糊搜索 |
| **包装管理** | 物料号、供应商、包装型号、包装容量 | 器具/包装数据维护 |
| **供应商管理** | 供应商代码、名称、联系人、电话 | 供应商档案 CRUD |
| **库区管理** | 库区代码、名称、排序 | 库区划分维护 |

### 2. 入库管理

从创建入库单到扫码入库的全流程，含看板标签体系。

| 功能 | 说明 |
| :--- | :--- |
| **入库单管理** | 创建入库单（供应商 → 物料 → 明细 → 保存），按单号/供应商/状态/日期筛选 |
| **看板标签生成** | 根据包装容量自动计算包数，为每包生成唯一看板号（`IN + yyyyMMdd + 序号 + 毫秒`） |
| **扫码入库** | 扫码/输入看板号 → 查询标签 → 确认入库 → 自动更新库存（Web + 移动端） |
| **手工入库** | 选择入库单 → 选择看板号 → 填写实收数量 → 确认 |
| **入库历史** | 按时间/物料/单据追溯入库记录 |
| **状态流转** | 未入库 → 部分完成 → 已完成（自动） |

### 3. 出库管理

支持带单出库、不带单出库、FIFO 先进先出。

| 功能 | 说明 |
| :--- | :--- |
| **出库单管理** | 创建出库单（供应商 → 物料卡片选择 → 明细），按单号/供应商/状态/日期筛选 |
| **看板选择出库** | 选择可出库看板列表 → 指定每个看板出库数量 → 批量出库 |
| **扫码出库** | 扫看板 → FIFO 校验 → 确认出库 → 扣减库存（Web + 移动端） |
| **不带单出库** | 移动端直接扫码出库，自动生成出库单 |
| **出库历史** | 按时间/物料/单据追溯出库记录，含状态列 |
| **状态流转** | 待出库 → 部分完成 → 已完成（自动） |

### 4. 退库管理

已出库看板退回仓库，库存回增。

| 功能 | 说明 |
| :--- | :--- |
| **扫码退库** | 扫看板号 → 查询退库资格（canReturn）→ 确认退库 → 库存回增（Web + 移动端） |
| **按单退库** | 出库单详情中选择已出库看板，批量退库 |
| **看板重置** | 退库后 labelStatus 恢复为"已入库"，看板可再次出库 |
| **出库单重算** | 自动更新出库单状态（全部退完 → 已退库） |

### 5. 转包管理

看板拆包（分出数量到新看板）和合包（合并到已有看板）。

| 功能 | 说明 |
| :--- | :--- |
| **拆包** | 自动新建 / 指定新建目标看板，从源看板转出部分数量 |
| **合包** | 源看板数量合并到已有目标看板 |
| **转包历史** | 完整记录每次转包操作（源/目标看板号、数量、操作人） |

### 6. 封存管理

封存/解封看板，被封存看板禁止出库。

| 功能 | 说明 |
| :--- | :--- |
| **封存/解封** | 单个看板封存/解封切换（Web + 移动端） |
| **批量操作** | 批量封存/解封多个看板 |
| **已封存列表** | 按物料号/供应商筛选已封存看板 |
| **出库拦截** | 已封存看板出库时前端弹提示 + 后端拒绝 |

### 7. 库存管理

实时反映各物料库存现状。

| 功能 | 说明 |
| :--- | :--- |
| **库存报表** | 按物料号/物料名称/供应商筛选，分页展示 |
| **看板查询** | 按物料+供应商+库区查看看板标签列表 |
| **库存维度** | 物料号 + 供应商 + 库区三维度 |
| **Excel 导出** | GET /api/export/inventory 导出库存报表 |

### 8. 物料需求管理

创建物料需求批次，追踪出库满足情况。

| 功能 | 说明 |
| :--- | :--- |
| **手工创建** | 选择物料 → 填写需求数量/日期 → 生成批次号 |
| **需求列表** | 分页查询，按状态/物料/供应商筛选 |
| **状态追踪** | 待出库 → 部分完成 → 已完成（出库时自动更新 fulfilledQty） |

### 9. 高低储预警

设定物料库存安全上下限，主动预警。

| 功能 | 说明 |
| :--- | :--- |
| **阈值配置** | 为每种物料+供应商设置低储/高储阈值 |
| **仪表盘预警** | 首页展示低储预警数 + 库存健康度百分比 |
| **健康度算法** | 逐物料按库区聚合库存，扣除封存量后与阈值对比 |

### 10. AI 仓库管理员

基于 DeepSeek 大语言模型的智能助手，缺货预测、呆滞预警、自然语言对话。

| 功能 | 说明 |
| :--- | :--- |
| **缺货预测** | 基于近 30 天出库消耗速率，计算日均消耗和可支撑天数，三级风险预警 |
| **呆滞报废预警** | 识别长期无出库物料，30/60/90 天分级告警 |
| **AI 对话助手** | Function Calling 自动查询数据库，Markdown 渲染回复 |
| **定时预警** | 每小时自动执行全量分析，结果持久化到 `ai_alert` 表 |

### 11. 仪表盘

登录后首页，数据驾驶舱。

| 指标 | 数据源 |
| :--- | :--- |
| 待入库单数 | 排除 status="已完成" |
| 待出库单数 | 排除 status="已完成" |
| 低储预警数 | inventory_stock 中 onHandQty=0 的数量 |
| 总物料数 | material_info count |
| 待办任务列表 | 未完成的入库单 + 出库单 |
| 库存健康度 | 按阈值配置对比统计 |

### 12. 操作日志审计

AOP 自动记录所有写操作。

| 功能 | 说明 |
| :--- | :--- |
| **自动拦截** | AOP 切面拦截所有 POST/PUT/DELETE，记录操作人/对象/类型/IP/时间 |
| **日志查询** | 按操作人/类型/时间分页筛选 |
| **不可删除** | 前端无删除入口，后端无删除接口 |

### 13. 移动端应用

uni-app 跨平台（Android APK + H5 + 微信小程序）。

| 页面 | 功能 |
| :--- | :--- |
| 登录 | 服务器地址动态配置 + 原生 input 兼容 |
| 首页 | 7 宫格主菜单导航 |
| 扫码入库 | 扫看板 → 确认入库 |
| 扫码出库 | 带单/不带单两种模式 + FIFO 预警 |
| 带单出库选单 | 列出待出库单据选单跳转 |
| 退库 | 扫看板 → 确认退库 |
| 转包 | 拆包/合包三种模式 + 二维码结果 |
| 封存/解封 | 扫码操作 Tab + 已封存列表 Tab |
| 设置 | 服务器地址修改 + 退出登录 |

### 14. 报表导出

Apache POI 生成 Excel。

| 导出接口 | 内容 |
| :--- | :--- |
| GET /api/export/inventory | 库存报表（物料/供应商/库区/库存/封存/可用） |
| GET /api/export/inbound | 入库看板明细 |
| GET /api/export/outbound | 出库历史明细 |
| GET /api/export/transfer | 转包记录 |

## 数据库设计

20 张表，均含审计字段（created_by / updated_by / created_at / updated_at）：

| 表名 | 说明 |
| :--- | :--- |
| sys_user | 系统用户 |
| supplier_info | 供应商 |
| material_info | 物料主数据 |
| packaging_info | 包装信息 |
| warehouse_area | 库区 |
| inbound_order | 入库单头 |
| inbound_order_detail | 入库单明细 |
| inbound_kanban_label | 入库看板标签 |
| inventory_stock | 库存快照 |
| outbound_order | 出库单头 |
| outbound_order_detail | 出库单明细 |
| outbound_history | 出库历史 |
| package_transfer | 转包记录 |
| demand_batch | 需求批次 |
| demand_detail | 需求明细 |
| alert_threshold | 高低储阈值 |
| ai_alert | AI 预警记录 |
| audit_log | 操作审计日志 |
| inventory_check_task | 盘点任务 |
| inventory_check_detail | 盘点明细 |

## API 文档

### 认证模块

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| POST | /api/auth/login | 登录 |
| POST | /api/auth/refresh | 刷新 Token |
| GET | /api/auth/userInfo | 获取用户信息 |
| POST | /api/auth/logout | 退出登录 |
| PUT | /api/auth/changePassword | 修改密码 |
| PUT | /api/auth/userInfo | 更新个人信息 |

### 入库模块

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | /api/inbound/orders | 入库单列表 |
| POST | /api/inbound/orders | 创建入库单 |
| GET | /api/inbound/orders/{id} | 入库单详情 |
| POST | /api/inbound/orders/{id}/receive | 手工入库 |
| POST | /api/inbound/orders/{id}/kanban-labels/generate | 生成看板标签 |
| GET | /api/inbound/orders/{id}/kanban-labels | 查看看板列表 |
| POST | /api/inbound/orders/{id}/receive-by-labels | 按看板入库 |
| GET | /api/inbound/scan/labels/{kanbanNo} | 扫码查询看板 |
| POST | /api/inbound/scan/receive | 扫码入库 |

### 出库模块

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | /api/outbound/orders | 出库单列表 |
| POST | /api/outbound/orders | 创建出库单 |
| GET | /api/outbound/orders/{id} | 出库单详情 |
| POST | /api/outbound/orders/{id}/issue | 执行出库 |
| GET | /api/outbound/orders/{id}/available-kanban-labels | 可出库看板列表 |
| POST | /api/outbound/orders/{id}/issue-by-labels | 按看板出库 |
| GET | /api/outbound/scan/labels/{kanbanNo} | 扫码查询出库看板 |
| POST | /api/outbound/scan/issue | 扫码出库 |
| POST | /api/outbound/scan/orderless-issue | 不带单出库 |

### 退库模块

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | /api/outbound/return/labels/{kanbanNo} | 查询退库信息 |
| POST | /api/outbound/return | 执行退库 |
| GET | /api/outbound/orders/{id}/issued-labels | 已出库看板列表 |
| POST | /api/outbound/orders/{id}/return-by-labels | 按看板退库 |

### 其他模块

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| GET | /api/dashboard/data | 仪表盘数据 |
| GET | /api/inventory/stocks | 库存报表 |
| GET/POST/PUT/DELETE | /api/basic/* | 基础数据 CRUD |
| GET/POST | /api/alert/thresholds | 预警阈值 |
| GET | /api/ai/alerts/latest | AI 最新预警 |
| POST | /api/ai/chat | AI 对话 |
| POST | /api/transfer/execute | 执行转包 |
| GET | /api/seal/label | 查询封存信息 |
| POST | /api/seal/toggle | 封存/解封 |
| POST | /api/demand/create | 创建需求 |
| GET | /api/audit/list | 操作日志 |
| GET | /api/export/* | Excel 导出 |

**统一响应格式：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1717200000000
}
```

## 项目结构

```
warehouse-management-system/
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/wms/
│           │   ├── controller/     # 18 个 REST Controller
│           │   ├── service/        # 12 个 Service 接口
│           │   ├── service/impl/   # 12 个 Service 实现
│           │   ├── repository/     # 20 个 Repository
│           │   ├── entity/         # 20 个 Entity
│           │   ├── dto/            # 44 个 DTO
│           │   ├── config/         # Security / JWT / CORS / Exception
│           │   └── aspect/         # AuditLogAspect
│           └── resources/
│               ├── application.yml
│               └── schema.sql
├── frontend/                       # Vue 3 前端
│   ├── src/
│   │   ├── views/                  # 27 个页面
│   │   ├── components/             # 13 个组件
│   │   ├── api/                    # 15 个 API 模块
│   │   ├── router/                 # 路由配置
│   │   ├── store/                  # Pinia Store
│   │   ├── layout/                 # 布局组件
│   │   └── utils/                  # 工具函数
│   └── vite.config.js
├── mobile/                         # uni-app 移动端
│   └── src/
│       ├── pages/                  # 9 个页面
│       ├── components/             # 3 个组件
│       ├── api/                    # 6 个 API 模块
│       ├── store/                  # Pinia Store
│       └── utils/                  # 工具函数
└── docs/                           # 项目文档
    ├── requirements/               # 需求分析文档（16 份）
    └── design/                     # 设计文档（16 份）
```

## 快速启动

### 后端
```bash
cd backend
mvn spring-boot:run                  # 启动后端 (端口 8080)
```

### 前端
```bash
cd frontend
npm install && npm run dev            # 启动前端 http://localhost:5173
```

### 移动端
```bash
cd mobile
npm install && npm run dev:app        # HBuilder X 中运行到手机
```

### 预置账号

| 用户名 | 密码 | 角色 |
| :--- | :--- | :--- |
| admin | admin123 | 系统管理员 |
| manager | admin123 | 仓库经理 |
| operator | admin123 | 操作员 |
