# 汽车物流WMS仓库管理系统

> 面向汽车物流行业的仓储管理系统，涵盖入库、出库、库存监控等核心业务，支持AI智能分析与移动端操作。

## 项目概述

本项目是一个面向汽车物流行业的仓储管理系统开发实训项目，涵盖基础信息管理、入库管理、出库管理、库存监控、高低储预警等核心业务模块，采用前后端分离架构。

## 技术栈

### 前端
- Vue 3
- Element Plus
- Axios（HTTP 请求）
- Vue Router（路由管理）
- Pinia（状态管理）
- Vite（构建工具）

### 后端
- Java JDK 1.8+
- Spring Boot 2.7.x
- Spring Security（认证与授权）
- Spring Data JPA（数据访问）
- MySQL 5.7+
- JWT（jjwt 0.9.1，无状态认证）
- BCrypt（密码加密）

## 功能模块

### 0. 系统登录与用户认证

提供用户登录、权限校验和会话管理功能，作为所有业务模块的入口。

| 功能 | 接口 | 说明 |
| :--- | :--- | :--- |
| **登录** | POST /api/auth/login | 用户名+密码登录，返回 JWT Token 和用户信息 |
| **获取用户信息** | GET /api/auth/userInfo | 根据 Token 获取当前登录用户信息 |
| **退出登录** | POST /api/auth/logout | 前端清除 Token 即可（无状态方案） |

**用户角色体系：**
- `admin` — 系统管理员，拥有全部权限
- `manager` — 仓库经理，审核与报表查看
- `operator` — 操作员，日常出入库操作

**登录数据流：**
```
用户输入账号密码 → 前端 POST /api/auth/login → 后端校验密码(BCrypt)
→ 生成JWT Token → 返回前端 → 前端存储 Token 到 localStorage
→ 后续请求在请求头携带 Authorization: Bearer <token>
→ 后端 JwtAuthFilter 校验 Token → 放行请求到 Controller
```

### 1. 基础信息管理

对系统运行所需的静态主数据进行统一维护，为入库、出库、库存监控提供基础数据支撑。

| 页面/功能 | 核心字段 | 详细说明 |
| :--- | :--- | :--- |
| **物料信息维护** | 物料号、物料名称、物料类型、单位、供应商 | 新增/编辑/查询/删除物料，支持模糊搜索与分页展示 |
| **包装信息维护** | 物料号、供应商、包装型号、包装容量 | 管理与物料关联的器具/包装数据 |
| **供应商信息维护** | 供应商代码、供应商名称、联系人、联系方式 | 统一管理供应商档案数据 |

### 2. 入库管理

支持从创建入库单到完成入库的全流程管理，实时追踪单据状态。

| 页面/功能 | 核心字段 | 详细说明 |
| :--- | :--- | :--- |
| **入库单管理** | 单号、供应商、零件明细、计划数量、状态 | 创建入库单 → 选择供应商 → 选择零件 → 输入计划入库数量 → 保存生成入库单；支持按单号/供应商/状态/日期组合查询 |
| **手工入库** | 入库单号、零件号、实际入库数量 | 选择入库单 → 录入实收数量 → 系统自动对比计划数量与实际数量，更新入库单状态（未入库 → 部分完成 → 已完成） |
| **入库单状态流转** | 未入库、部分完成、已完成 | 系统根据实际入库数量与计划入库数量的对比结果自动推进状态 |

### 3. 出库管理

支持从创建出库单到完成出库的全流程管理。

| 页面/功能 | 核心字段 | 详细说明 |
| :--- | :--- | :--- |
| **出库单管理** | 单号、供应商、零件明细、计划数量、状态 | 创建出库单 → 选择供应商 → 选择零件 → 输入计划出库数量 → 保存生成出库单 |
| **手工出库** | 出库单号、零件号、实际出库数量 | 选择出库单 → 录入实发数量 → 系统实时校验库存是否充足，扣减库存并更新单据状态 |

### 4. 库存管理

实时反映各物料库存现状，监控库存健康度。

| 页面/功能 | 核心字段 | 详细说明 |
| :--- | :--- | :--- |
| **库存报表** | 物料号、物料名称、供应商、当前库存、低储天数、高储天数、预警状态 | 实时展示全量物料库存数据，支持按物料号/供应商筛选；列表直观标注低储预警（库存低于安全线）与高储预警（库存超过上限） |
| **需求导入** | 零件号、需求量、需求日期 | 支持 Excel/CSV 批量导入零件需求计划数据，为出库计划提供依据 |

### 5. 物料需求 / 出库记录

满足计划性出库场景，支持批量导入需求并追踪后续出库执行结果。

| 页面/功能 | 核心字段 | 详细说明 |
| :--- | :--- | :--- |
| **导入零件需求** | 零件号、物料名称、需求数量、需求日期、导入批次号 | 下载模板 → 填写需求数据 → 上传导入；系统记录每次导入历史，支持按批次查询 |
| **出库记录查询** | 出库单号、物料号、出库数量、出库日期、操作人 | 按时间/物料/单据维度追溯出库历史 |

### 6. 高低储预警

通过设定物料的安全库存上下限，实现主动式库存风险预警。

| 页面/功能 | 核心字段 | 详细说明 |
| :--- | :--- | :--- |
| **高低储标准维护** | 物料号、低储天数、高储天数 | 为每种物料分别设定低储天数（低于该天数触发低储预警）和高储天数（高于该天数触发高储预警） |
| **预警展示** | 物料号、当前库存、低储天数、高储天数、预警标识 | 在库存报表中以颜色/图标区分正常、低储、高储三种状态，支持快速筛选异常物料 |

### 7. AI 仓库管理员 🆕

基于 DeepSeek 大语言模型的智能仓库管理助手，实现物料缺货预测、呆滞报废预警和自然语言交互。

| 功能 | 说明 |
| :--- | :--- |
| **缺货预测** | 基于近 30 天出库消耗速率，自动计算每种物料的日均消耗量和可支撑天数，按 HIGH / MEDIUM / LOW 三级风险预警 |
| **呆滞报废预警** | 识别长期无出库记录的物料，按呆滞天数（30/60/90 天）分级告警，生成处置建议 |
| **AI 对话助手** | 支持自然语言提问，AI 通过 Function Calling 自动查询数据库（库存/出入库历史/预警记录），生成分析报告 |
| **定时预警** | 后台每小时自动执行全量分析，结果持久化到 `ai_alert` 表，Dashboard 卡片实时展示 |
| **多入口访问** | Dashboard 预警卡片 + 全局悬浮按钮 + 独立 AI 对话页面，三入口覆盖不同使用场景 |

**AI 对话入口：**
- **Dashboard 预警卡片** — 仪表盘中部展示缺货/呆滞风险 Top 5，点击跳转 AI 分析
- **全局悬浮按钮** — 右下角蓝色图标，任意页面呼出迷你聊天面板
- **独立对话页面** — 侧边栏「AI 助手」菜单，全屏对话 + Markdown 渲染

> 📖 详细文档见 [AI仓库管理员说明文档.md](AI仓库管理员说明文档.md)

### 8. 扩展功能（待定）

> 可选加分项，功能设计待后续补充。

## 数据库设计

### 核心数据表

所有表均包含统一基础列：创建人(created_by)、更新人(updated_by)、创建时间(created_at)、更新时间(updated_at)

| 序号 | 表名 | 核心字段 | 说明 |
| :--- | :--- | :--- | :--- |
| 0 | **sys_user**（认证用） | id、username、password、real_name、role、status | 系统用户表，用于登录认证 |
| 1 | 物料表 | id、物料号、物料名称、供应商 | 物料主数据 |
| 2 | 器具表 | id、物料号、供应商、包装型号、包装容量 | 包装器具数据 |
| 3 | 库存表 | id、物料号、库存、低储天数、高储天数 | 库存快照 |
| 4 | 入库单 | id、单号、入库单状态（已完成/部分完成/未入库） | 入库单头 |
| 5 | 入库单明细 | id、入库单id、入库单号、零件号、包装容量、计划入库数量、实际入库数量 | 入库单行 |
| 6 | 出库单 | id、单号、出库单状态 | 出库单头 |
| 7 | 出库单明细 | id、出库单id、出库单号、零件号、包装容量、计划出库数量、实际出库数量 | 出库单行 |
| 8 | 条码表（可选） | id、物料号、供应商、条码号、出入库状态 | 条码管理 |
| 9 | 供应商信息 | id、供应商代码、供应商名称 | 供应商主数据 |
| 10 | **ai_alert** 🆕 | id、物料号、预警类型、风险等级、日均消耗、可支撑天数、呆滞天数、建议 | AI 预警记录 |

## 核心业务流程

### 登录流程
```
用户访问系统 → 输入账号密码 → 后端校验(BCrypt) → 生成JWT → 返回Token → 前端存储 → 后续请求携带Token
```

### 入库流程
```
浏览/创建入库单 → 选择供应商 → 零件选择 → 输入数量 → 保存入库单 → 做入库
```

### 出库流程
```
做入库后查看库存 → 创建出库单 → 出库选择供应商 → 出库零件选择 → 输入出库数量 → 生成出库单
```

## API 文档

### 认证模块

| 方法 | 路径 | 说明 | 是否需要Token |
| :--- | :--- | :--- | :--- |
| POST | /api/auth/login | 登录 | 否 |
| GET | /api/auth/userInfo | 获取当前用户信息 | 是 |
| POST | /api/auth/logout | 退出登录 | 是 |

**登录请求示例：**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**登录成功响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ...",
    "tokenType": "Bearer",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "系统管理员",
      "role": "admin",
      "avatar": null
    }
  },
  "timestamp": 1717200000000
}
```

### AI 模块

| 方法 | 路径 | 说明 | 是否需要Token |
| :--- | :--- | :--- | :--- |
| POST | /api/ai/chat | AI 对话（透传 DeepSeek，支持 Function Calling） | 是 |
| GET | /api/ai/alerts/latest | 获取最新预警结果（支持 ?alertType=SHORTAGE\|DEAD_STOCK） | 是 |
| GET | /api/ai/data/stocks | 全量库存快照（供 AI Function Calling） | 是 |
| GET | /api/ai/data/outbound-history | 出库历史（支持 ?materialCode= 筛选） | 是 |
| GET | /api/ai/data/inbound-history | 入库历史（支持 ?materialCode= 筛选） | 是 |

**请求头格式（需Token的接口）：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ...
```

## 项目结构

```
warehouse-management-system/
├── backend/                    # Spring Boot后端项目
│   ├── pom.xml                 # Maven依赖配置
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/wms/
│           │       ├── controller/     # REST API控制器
│           │       │   └── AuthController.java
│           │       ├── service/        # 业务逻辑层
│           │       │   ├── UserService.java
│           │       │   └── impl/UserServiceImpl.java
│           │       ├── repository/     # 数据访问层
│           │       │   └── UserRepository.java
│           │       ├── entity/         # 实体类
│           │       │   ├── BaseEntity.java
│           │       │   ├── SysUser.java
│           │       │   └── AiAlert.java              🆕 AI 预警记录
│           │       ├── dto/            # 数据传输对象
│           │       │   ├── ApiResult.java
│           │       │   ├── LoginRequest.java
│           │       │   ├── LoginResponse.java
│           │       │   ├── UserInfoDTO.java
│           │       │   └── DashboardDTO.java
│           │       ├── config/         # 配置类
│           │       │   ├── SecurityConfig.java
│           │       │   ├── JwtUtil.java
│           │       │   ├── JwtAuthFilter.java
│           │       │   ├── CorsConfig.java
│           │       │   ├── GlobalExceptionHandler.java
│           │       │   └── AiScheduler.java          🆕 AI 定时预警调度
│           │       ├── controller/     # REST API控制器
│           │       │   ├── AuthController.java
│           │       │   ├── BasicDataController.java
│           │       │   ├── DashboardController.java
│           │       │   ├── InboundOrderController.java
│           │       │   ├── InboundScanController.java
│           │       │   ├── InventoryController.java
│           │       │   ├── OutboundOrderController.java
│           │       │   ├── OutboundScanController.java
│           │       │   └── AiChatController.java     🆕 AI 对话代理 + 数据API
│           │       ├── service/        # 业务逻辑层
│           │       │   ├── UserService.java
│           │       │   ├── BasicDataService.java
│           │       │   ├── DashboardService.java
│           │       │   ├── InboundOrderService.java
│           │       │   ├── InventoryService.java
│           │       │   ├── OutboundOrderService.java
│           │       │   ├── AiAlertService.java       🆕 AI 预警计算引擎
│           │       │   └── impl/
│           │       ├── repository/     # 数据访问层
│           │       │   ├── UserRepository.java
│           │       │   ├── AiAlertRepository.java    🆕 AI 预警仓库
│           │       │   └── ...（共 11 个）
│           │       └── WmsApplication.java
│           └── resources/
│               ├── application.yml     # 应用配置
│               ├── application-local.yml  🆕 本地配置（API Key，不提交git）
│               └── schema.sql          # 数据库初始化脚本（含 ai_alert 表）
└── frontend/                   # Vue 3前端项目
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js                     # 应用入口
        ├── App.vue                     # 根组件（路由出口）
        ├── layout/                     # 布局组件
        │   ├── Layout.vue              # 主布局（侧边栏 + 顶栏 + 内容区 + AI悬浮按钮）
        │   ├── Sidebar.vue             # 侧边导航菜单（含「AI 助手」菜单项）
        │   └── Navbar.vue              # 顶部导航栏（面包屑/用户下拉/预警）
        ├── views/                      # 页面视图（按模块划分子目录）
        │   ├── Login.vue              # 登录页
        │   ├── Dashboard.vue          # 仪表盘（统计卡片/待办/AI预警卡片/快捷操作）
        │   ├── basic/                 # 基础信息管理
        │   │   ├── Material.vue       # 物料管理
        │   │   ├── Packaging.vue      # 包装管理
        │   │   └── Supplier.vue       # 供应商管理
        │   ├── inbound/               # 入库管理
        │   │   ├── Order.vue          # 入库单管理
        │   │   ├── Manual.vue         # 手工入库
        │   │   ├── Scan.vue           # 扫码入库
        │   │   └── History.vue        # 入库历史
        │   ├── outbound/              # 出库管理
        │   │   ├── Order.vue          # 出库单管理
        │   │   ├── Manual.vue         # 手工出库
        │   │   ├── Scan.vue           # 扫码出库
        │   │   └── History.vue        # 出库历史
        │   ├── inventory/             # 库存管理
        │   │   ├── Report.vue         # 库存报表
        │   │   └── ImportReq.vue      # 需求导入
        │   ├── demand/                # 物料需求
        │   │   └── List.vue           # 物料需求 / 出库记录
        │   ├── alert/                 # 高低储预警
        │   │   └── Threshold.vue      # 预警标准维护
        │   └── ai/                    🆕 AI 助手
        │       └── Chat.vue           # AI 对话全屏页面
        ├── components/                 # 公共组件
        │   ├── PageContainer.vue       # 通用页面容器
        │   ├── inbound/                # 入库子组件
        │   ├── outbound/               # 出库子组件
        │   └── ai/                     🆕 AI 子组件
        │       ├── AiAlertCards.vue    # Dashboard AI 预警卡片
        │       └── AiFloatingButton.vue # 全局悬浮 AI 聊天按钮
        ├── api/                        # API接口封装
        │   ├── request.js              # Axios实例（拦截器/Token/错误处理）
        │   ├── auth.js                 # 认证相关
        │   ├── basic.js                # 基础数据
        │   ├── dashboard.js            # 仪表盘
        │   ├── inbound.js              # 入库
        │   ├── outbound.js             # 出库
        │   ├── inventory.js            # 库存
        │   └── ai.js                   🆕 AI 对话 + 预警
        ├── store/                      # Pinia状态管理
        │   └── user.js                 # 用户认证状态
        ├── router/                     # Vue Router路由配置
        │   └── index.js                # 路由表 + 导航守卫（含 /ai/chat）
        └── utils/                      # 工具函数
            └── auth.js                 # Token/localStorage存取

## 预置账号

| 用户名 | 密码 | 角色 | 说明 |
| :--- | :--- | :--- | :--- |
| admin | admin123 | admin | 系统管理员，拥有全部权限 |
| manager | admin123 | manager | 仓库经理，审核与报表查看 |
| operator | admin123 | operator | 操作员，日常出入库操作 |

## 开发环境搭建

### 后端环境

1. **环境要求：** Java JDK 1.8+、Maven 3.6+、MySQL 5.7+
2. **创建数据库：** `CREATE DATABASE wms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
3. **修改配置：** 编辑 `backend/src/main/resources/application.yml`，修改 MySQL 连接信息
4. **启动后端：**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
5. **验证服务：** 访问 `http://localhost:8080/api/auth/login`，POST 方式测试登录

### 前端环境

1. 确保安装 Node.js 和 npm
2. 进入 frontend 目录
3. 运行 `npm install` 安装依赖
4. 运行 `npm run dev` 启动前端开发服务器

## 特色功能

- **AI 仓库管理员** 🆕：基于 DeepSeek 大模型，缺货预测 + 呆滞报废预警 + 自然语言对话，三大入口（Dashboard 卡片 / 悬浮按钮 / 独立页面），Function Calling 自动查询数据库
- **定时智能预警**：每小时自动分析全量库存，三级风险分级（HIGH/MEDIUM/LOW），生成处置建议
- **BI数据分析**：库存健康度分析、有效期情况统计
- **条码化管理**：支持手持扫码 + 看板二维码标签，提升作业效率
- **全流程状态跟踪**：入库单/出库单状态（未完成/部分完成/已完成）精确追踪，FIFO 先进先出
- **JWT无状态认证**：基于Token的认证方案，无需Session，适合前后端分离与移动端扩展

## 开发要求

- **前端**：使用 Vue 3 + Element Plus 实现响应式界面，包含表单、表格、图表等组件
- **后端**：使用 Spring Boot 构建 RESTful API，实现业务逻辑和数据校验
- **数据库**：使用 MySQL 设计关系型数据结构，确保数据一致性
- **认证**：所有非登录接口需在请求头携带 JWT Token，后端通过 Spring Security + Filter 统一鉴权
- **AI 大模型**：集成 DeepSeek API，通过 Function Calling 实现数据库自动查询与智能分析，API Key 存放在 `application-local.yml`（已加入 .gitignore）

## 备注

- 所有数据表需包含审计字段（创建人、更新人、创建时间、更新时间）
- AI 仓库管理员功能已实现（基于 DeepSeek），API Key 配置见 `application-local.yml`，详细文档见 [AI仓库管理员说明文档.md](AI仓库管理员说明文档.md)
- 系统需支持入库、库存监控、出库管理三大核心菜单页面的熟练开发
- 首次启动会自动执行 schema.sql 创建表结构并初始化三个预置账号
- 如果启动时遇到 MySQL 时区问题，可在连接 URL 后添加 `&serverTimezone=Asia/Shanghai`
