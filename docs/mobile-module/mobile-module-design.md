# WMS 手机端设计文档

> 版本 v1.1 | 2026-06-24（实现后更新）

## 1. 概述

为 WMS 仓库管理系统增加手机端（Android APK），供仓库操作员手持终端完成扫码入库、扫码出库、带单出库、退库等核心作业。PC 端继续负责建单、报表、主数据维护等复杂操作。

### 1.1 目标用户

仓库操作员（operator 角色），主要使用场景：手持 PDA/手机在库区扫描看板标签进行出入库作业。

### 1.2 功能清单

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 登录 | ✅ 实现 | 复用 PC 端账号密码 / JWT Token |
| 扫码入库 | ✅ 实现 | 扫看板号 → 确认 → 调用现有入库接口 |
| 扫码出库 | ✅ 实现 | 扫看板号 → FIFO 预警弹窗 → 确认 → 自动创建出库单 + 扣库存 + 写出库历史 |
| 带单出库 | ✅ 实现 | 选已有待出库/部分完成出库单 → 扫看板号 → FIFO 预警 → 确认 |
| 退库 | ✅ 实现 | 扫看板号 → 确认 → 库存回增 + 出库历史标记已退库 |
| 转包 | 🔲 占位 | 首页放按钮，逻辑待定 |
| 封存 | 🔲 占位 | 首页放按钮，逻辑待定 |
| 解封 | 🔲 占位 | 首页放按钮，逻辑待定 |

---

## 2. 技术选型

| 层 | 选型 | 说明 |
|----|------|------|
| 框架 | uni-app (Vue 3 模式) | Vue 3 语法，编译为 Android APK |
| UI 库 | Vant 4 | 移动端专用组件库 |
| 扫码 | `uni.scanCode()` | uni-app 内置 API，调系统相机扫二维码 |
| HTTP | `uni.request()` + 拦截器封装 | 替代 Axios，同样注入 JWT Token |
| 存储 | `uni.storage` / `uni.storageSync` | 替代 localStorage |
| 状态管理 | Pinia | 与 PC 端一致 |
| 构建 | HBuilder X 云打包 | 编译 .apk |

---

## 3. 项目结构

```
Warehouse-Management-System/
├── mobile/                          # 新建手机端项目（uni-app）
│   ├── manifest.json                # uni-app 配置（AppID/权限/图标/相机权限）
│   ├── pages.json                   # 页面路由配置
│   ├── App.vue                      # 应用入口
│   ├── main.js                      # 全局注册
│   ├── index.html                    # uni-app Vue 3 入口（必需）
│   │
│   ├── api/
│   │   ├── request.js               # uni.request 封装（baseURL/JWT拦截器/错误处理）
│   │   ├── auth.js                  # 登录 API
│   │   ├── inbound.js               # 扫码入库 API
│   │   └── outbound.js              # 扫码出库 / 带单出库 / 退库 API
│   │
│   ├── pages/
│   │   ├── login/index.vue          # 登录页
│   │   ├── home/index.vue           # 首页（功能入口 Grid）
│   │   ├── inbound-scan/index.vue   # 扫码入库
│   │   ├── outbound-scan/index.vue  # 扫码出库
│   │   ├── outbound-order/index.vue # 带单出库（选单 + 扫码）
│   │   ├── return/index.vue         # 退库
│   │   └── settings/index.vue       # 设置页（服务器地址 / 退出登录）
│   │
│   ├── components/
│   │   ├── ScanInput.vue            # 通用扫码输入组件（输入框 + 扫码按钮）
│   │   ├── MaterialCard.vue         # 物料信息展示卡片
│   │   └── FifoAlert.vue            # FIFO 预警弹窗
│   │
│   ├── store/
│   │   └── user.js                  # Pinia 用户状态（token/角色/用户信息）
│   │
│   └── utils/
│       └── auth.js                  # Token 存取（uni.storage）
└── docs/mobile-module/
    └── mobile-module-design.md      # 本文档
```

---

## 4. 页面设计

### 4.1 导航结构

```
登录页
  │
  └─ 首页（Vant Grid 宫格入口）
       ├── [卡片] 扫码入库  ──→ 扫码入库页
       ├── [卡片] 扫码出库  ──→ 扫码出库页
       ├── [卡片] 带单出库  ──→ 选择出库单页 → 扫码出库页
       ├── [卡片] 退库      ──→ 扫码退库页
       ├── [卡片] 转包      ──→ 占位提示
       ├── [卡片] 封存      ──→ 占位提示
       └── [卡片] 解封      ──→ 占位提示
```

底部导航栏：**首页** / **扫码**（快捷入口直接跳扫码出库页）

### 4.2 扫码功能页统一布局（入库/出库/退库）

```
┌──────────────────────────────┐
│  ← 返回    功能名称          │  ← 顶部 NavBar
├──────────────────────────────┤
│                              │
│  ┌────────────────────┐ [📷] │  ← ScanInput（输入框 + 扫码按钮）
│  │ 输入/扫码看板号     │      │     📷 按钮 → 调起系统相机
│  └────────────────────┘      │
│                              │
│  ─── 扫码识别后展示 ───     │
│                              │
│  ┌ 物料信息卡片 ──────────┐  │  ← MaterialCard
│  │ 物料号：MAT-ELE-001    │  │
│  │ 物料名：控制器模块      │  │
│  │ 供应商：宁波电子模组    │  │
│  │ 数量：8 件             │  │
│  │ 库区：默认库区          │  │
│  └────────────────────────┘  │
│                              │
│  ┌──────────┐ ┌──────────┐  │
│  │   确认    │ │   取消    │  │
│  └──────────┘ └──────────┘  │
│                              │
└──────────────────────────────┘
```

### 4.3 出库/带单出库页 FIFO 预警

出库和带单出库的扫码查询返回中，若 `fifoWarning: true`，弹出 **FifoAlert 弹窗**：

```
┌─────────────────────────┐
│  ⚠ FIFO 先进先出预警    │
├─────────────────────────┤
│                         │
│  当前出库的库存并非最早  │
│  入库批次，是否继续出库？│
│                         │
│  最早入库单号：IN2024... │
│                         │
│  ┌────────┐ ┌────────┐  │
│  │ 继续出库│ │ 取消   │  │
│  └────────┘ └────────┘  │
└─────────────────────────┘
```

用户点"继续出库" → 执行出库；点"取消" → 关闭弹窗，不执行操作。

### 4.4 带单出库选单页

```
┌──────────────────────────────┐
│  ← 返回    选择出库单        │
├──────────────────────────────┤
│  ┌──────────────────────┐    │
│  │ 🔍 搜索出库单号       │    │
│  └──────────────────────┘    │
│                              │
│  ┌ 出库单卡片 ────────────┐  │
│  │ OUT20240601001          │  │
│  │ 供应商：武汉东风         │  │
│  │ 状态：待出库             │  │
│  │ 明细：控制器模块 30件    │  │
│  └──────────────────────────┘  │
│                              │
│  ┌ 出库单卡片 ────────────┐  │
│  │ OUT20240601002          │  │
│  │ ...                     │  │
│  └──────────────────────────┘  │
└──────────────────────────────┘
```

选好出库单 → 跳转扫码出库页，后续流程与扫码出库一致，区别在于确认时请求体带 `outboundOrderId`。

### 4.5 登录页

与 PC 端风格一致，账号密码输入 + 登录按钮，登录成功存储 token，跳转首页。

---

## 5. 功能流程

### 5.1 扫码入库

```
输入/扫看板号（手动输入 or 扫码自动填充）
  ↓
GET /api/inbound/scan/labels/{kanbanNo}
  ↓ 成功返回
显示 MaterialCard（物料号/名称/供应商/数量/库区/看板状态）
  ↓ 检查 labelStatus
  ├─ "已入库" → 按钮禁用，提示"该看板已入库"
  └─ "未入库" → 确认按钮可用
       ↓ 点击确认
POST /api/inbound/scan/receive  { kanbanNo }
  ↓ 成功
清空页面，等待下一单
```

### 5.2 扫码出库

```
输入/扫看板号
  ↓
GET /api/outbound/scan/labels/{kanbanNo}
  ↓ 成功返回（含 fifoWarning / fifoMessage / earliestDocNo）
显示 MaterialCard + 库存信息
  ↓ 检查 fifoWarning
  ├─ true  → 弹出 FifoAlert 弹窗 → 用户点"继续出库" → 继续
  ├─ false → 直接可确认
  └─ 用户点"取消" → 关闭弹窗，停留当前页
  ↓ 点击确认
POST /api/outbound/scan/issue  { kanbanNo, issueQty }
  ↓ 成功（后端自动创建出库单、扣库存、写历史、状态已完成）
清空页面，等待下一单
```

### 5.3 带单出库

```
【步骤1：选择出库单】
GET /api/outbound/orders?status=待出库,部分完成
  ↓
展示出库单列表（支持按单号搜索）
  ↓ 点击某张出库单
【步骤2：进入扫码页（同5.2）】
  ↓
POST /api/outbound/scan/issue  { kanbanNo, issueQty, outboundOrderId }
  ↓ 成功
清空页面，返回首页
```

### 5.4 退库

```
输入/扫看板号
  ↓
GET /api/outbound/return/labels/{kanbanNo}   ← 新接口
  ↓ 成功返回
显示 MaterialCard + 原出库单号 + 出库时间
  ↓ 检查 canReturn
  ├─ false → 按钮禁用，提示原因（已退库/不存在）
  └─ true  → 确认按钮可用
       ↓ 点击确认
POST /api/outbound/return  { kanbanNo }       ← 新接口
  ↓ 成功（库存回增 + 出库历史状态 → 已退库）
清空页面，等待下一单
```

---

## 6. API 接口

### 6.1 复用现有接口

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/userInfo` | 获取用户信息 |
| GET | `/api/inbound/scan/labels/{kanbanNo}` | 扫码入库 → 查询看板信息 |
| POST | `/api/inbound/scan/receive` | 扫码入库 → 执行收货 |
| GET | `/api/outbound/scan/labels/{kanbanNo}` | 扫码出库 → 查询看板信息（含 FIFO） |
| POST | `/api/outbound/scan/issue` | 扫码出库 → 执行出库 |
| GET | `/api/outbound/orders?status=xxx` | 带单出库 → 列出库单列表 |

### 6.2 新增：退库查询

```
GET /api/outbound/return/labels/{kanbanNo}
```

**返回示例**：
```json
{
  "code": 200,
  "data": {
    "kanbanNo": "WMS-INBOUND|KB-xxx",
    "materialCode": "MAT-ELE-001",
    "materialName": "控制器模块",
    "supplierName": "宁波电子模组",
    "outboundDocNo": "OUT20240610001",
    "issueQty": 8,
    "issuedAt": "2026-06-10 10:00:00",
    "warehouseArea": "默认库区",
    "canReturn": true
  }
}
```

`canReturn: false` 的情况：
- 看板号在 `inbound_kanban_label` 中不存在
- 看板号对应的出库记录在 `outbound_history` 中不存在
- 出库记录 `status` 已经是"已退库"

**返回错误**：
```json
{ "code": 400, "message": "该看板已退库，无法重复退库" }
```

### 6.3 新增：执行退库

```
POST /api/outbound/return
```

**请求体**：
```json
{
  "kanbanNo": "WMS-INBOUND|KB-xxx"
}
```

**成功返回**：
```json
{
  "code": 200,
  "message": "退库成功",
  "data": {
    "outboundDocNo": "OUT20240610001",
    "materialCode": "MAT-ELE-001",
    "returnQty": 8,
    "currentStock": 98
  }
}
```

**后端操作**：
1. 根据 `kanbanNo` 查 `outbound_history` 找到对应的出库记录
2. `outbound_history.status` 更新为"已退库"
3. `inventory_stock.on_hand_qty` + 退库数量
4. 如果整张出库单所有明细均已退库 → `outbound_order.status` 更新为"已退库"

---

## 7. 数据库变更

在 `outbound_history` 表增加状态字段：

```sql
ALTER TABLE outbound_history
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT '已出库'
COMMENT '状态：已出库/已退库';

-- 新增出库单退库状态
ALTER TABLE outbound_order
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '待出库'
COMMENT '状态：待出库/部分完成/已完成/已退库';
```

---

## 8. 组件设计

### 8.1 ScanInput

```
Props:
  - placeholder: String        // 输入框占位文字
  - autoFocus: Boolean         // 进入页面自动聚焦

Events:
  - @scan(kanbanNo: String)    // 手动输入回车或扫码成功后触发

功能：
  - 左侧输入框：支持手动输入看板号，回车触发 @scan
  - 右侧相机按钮：点击 → uni.scanCode({ onlyFromCamera: true }) → 结果自动填入
```

### 8.2 MaterialCard

```
Props:
  - materialCode: String
  - materialName: String
  - supplier: String
  - qty: Number
  - warehouseArea: String
  - extra: Object              // 额外展示行（如出库单号/出库时间/标签状态等）

Slots:
  - footer                     // 默认放确认/取消按钮
```

### 8.3 FifoAlert

```
Props:
  - visible: Boolean           // 是否显示
  - message: String            // 预警文本
  - earliestDocNo: String      // 最早入库单号

Events:
  - @confirm                   // 用户点"继续出库"
  - @cancel                    // 用户点"取消"
```

---

## 9. 实现注意事项

### 9.1 uni-app Vue 3 配置要点

- **`index.html`**：uni-app Vue 3 模式必需，作为 Vite 入口文件，不可省略
- **`manifest.json`**：必须声明 `"vueVersion": "3"`，否则默认 Vue 2 编译失败
- **`pages.json`**：`easycom` 配置 `"^van-(.*)": "vant/es/$1/index.mjs"` 实现按需自动导入
- **`package.json`**：需显式依赖 `vue: ^3.4.0`，不可省略

### 9.2 移动端兼容性问题

**Vant 组件在 App webview 中的问题：**
- `van-field` 的 `v-model` 在手机 App 中不触发数据绑定 → **改用原生 `<input>` + `v-model`**
- `van-button` 的 `@click` 在手机 App 中有概率不触发 → **改用原生 `<button>` + `@tap`**
- `van-form` 的 `@submit` 在手机 App 中不触发 → 改用按钮 `@click` 直接调用方法
- 以上问题在浏览器 H5 模式中不存在，仅在真机 App webview 中出现

**扫码组件（ScanInput）：**
- 使用原生 `<input>` + 两个按钮（扫码 / 查询）
- 扫码支持 `WMS-INBOUND|` 前缀自动去除
- 浏览器调试时 `uni.scanCode()` 不可用，只能手动输入

**页面兼容性处理：**
- 登录页：完全使用原生 HTML 元素，零 Vant 依赖
- 其他页面：Vant 组件 + easycom 自动导入 + 显式 import 双保险

### 9.3 后端兼容性问题

- **状态过滤**：`GET /api/outbound/orders?status=待出库,部分完成` 不支持逗号分隔多状态（后端用 `equal()` 精确匹配），移动端改为前端侧过滤
- **出库数量计算**：退库后 `availableQty` 计算需排除 `status='已退库'` 的历史记录，4 处代码均需过滤
- **看板复用**：退库后 `labelStatus` 需重置为"已入库"，否则同一看板无法再次出库

### 9.4 接口变更

实际使用接口与设计文档差异：

| 设计 | 实际 | 原因 |
|------|------|------|
| `POST /api/outbound/scan/issue`（不带单出库） | `POST /api/outbound/scan/orderless-issue` | 远程新增，参数为物料信息而非看板号 |
| 带单出库参数 `outboundOrderId` | 两种方式均支持 | 远程新增 `outboundDocNo` 字段 |
| `GET /api/outbound/orders?status=待出库,部分完成` | 不传 status，前端过滤 | 后端不支持逗号分隔 |

---

## 10. 待定功能（占位）

以下功能只放按钮入口，点击后弹出"功能开发中"提示：

- **转包**：将看板标签中的部分物料转移到新包装 → 生成新看板标签
- **封存**：冻结指定库区/物料的库存，禁止出库
- **解封**：解除封存状态

---

## 11. 网络配置

开发阶段：
- 手机与开发电脑连接同一 WiFi
- `mobile/api/request.js` 中 `BASE_URL` 配置为电脑局域网 IP（如 `http://192.168.1.216:8080`）
- 后端 Spring Boot 默认绑定所有网络接口（`0.0.0.0`），无需额外配置
- CORS 已配置 `addAllowedOriginPattern("*")`，无需修改
- 浏览器 H5 调试时 `localhost:8080` 也可以使用

生产阶段：
- 后端部署到内网服务器，baseURL 改为服务器地址
- App 内设置页（`/pages/settings/index`）支持用户手动配置服务器地址
