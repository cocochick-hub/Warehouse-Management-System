# WMS 手机端 — 小组成员上手指南

> 适用对象：需要运行、调试或继续开发手机端的前后端开发者

## 1. 这是什么

`mobile/` 是一个独立的 **uni-app (Vue 3)** 项目，编译为 **Android APK**，供仓库操作员手持 PDA/手机进行扫码出入库作业。它和现有的 Web 前端（`frontend/`）共享同一个 Spring Boot 后端。

## 2. 环境准备

| 工具 | 用途 | 获取方式 |
|------|------|----------|
| HBuilder X | uni-app IDE，编译/打包/调试 | [dcloud.io/hbuilderx.html](https://www.dcloud.io/hbuilderx.html) |
| Node.js | npm 依赖管理 | 建议 v16+ |
| Android 手机 | 真机调试 / APK 安装测试 | 连接同 WiFi |
| MySQL + 后端 | 数据服务 | 先在电脑上启动后端 |

## 3. 快速启动

### 3.1 启动后端

```bash
cd backend
mvn spring-boot:run
# 确认 http://localhost:8080 可访问
```

### 3.2 启动手机端

**方式一：浏览器调试（开发推荐）**
1. HBuilder X → 文件 → 打开目录 → 选择 `mobile/`
2. 运行 → 运行到浏览器 → Chrome
3. F12 打开开发者工具查看请求

**方式二：真机调试**
1. 手机用 USB 连接电脑，开启 USB 调试
2. HBuilder X → 运行 → 运行到手机或模拟器
3. 手机上会自动安装调试版 App

**方式三：打包 APK**
1. HBuilder X → 发行 → 原生App-云打包
2. 等待云端构建完成 → 下载 .apk
3. 传到手机安装

### 3.3 配置服务器地址

- 手机和电脑必须连**同一个 WiFi**
- 查看电脑 IP：命令行 `ipconfig`，找到 IPv4 地址（如 `192.168.1.216`）
- 修改 `mobile/api/request.js` 中的 `BASE_URL`：
  ```js
  const BASE_URL = 'http://192.168.1.216:8080'
  ```
- 或者：App 内设置页（右上角齿轮图标）可以手动改地址

## 4. 功能清单

| 功能 | 入口 | 说明 |
|------|------|------|
| 扫码入库 | 首页 → 扫码入库 | 扫看板号 → 显示物料信息 → 确认入库 |
| 扫码出库 | 首页 → 扫码出库 | 扫看板号 → FIFO预警 → 确认出库（自动生成出库单） |
| 带单出库 | 首页 → 带单出库 | 先选已有出库单 → 再扫看板号 → FIFO预警 → 确认出库 |
| 退库 | 首页 → 退库 | 扫看板号 → 显示原出库单信息 → 确认退库（库存回增） |
| 转包 | 首页 → 转包 | 🔲 占位（提示"功能开发中"） |
| 封存 | 首页 → 封存 | 🔲 占位 |
| 解封 | 首页 → 解封 | 🔲 占位 |
| 设置 | 首页右上角齿轮 | 修改服务器地址 / 退出登录 |

## 5. 项目结构

```
mobile/
├── index.html              # uni-app Vue 3 入口（必须有）
├── manifest.json           # App 配置（vueVersion/权限/图标）
├── pages.json              # 页面路由 + easycom 自动导入配置
├── package.json            # 依赖（vue/pinia/vant）
├── App.vue                 # 应用根组件（启动鉴权）
├── main.js                 # 全局注册
│
├── api/
│   ├── request.js          # uni.request 封装（JWT注入/错误处理/baseURL）
│   ├── auth.js             # 登录/用户信息 API
│   ├── inbound.js          # 入库扫码 API
│   └── outbound.js         # 出库/退库 API
│
├── pages/
│   ├── login/              # 登录页（原生 input，零 Vant 依赖）
│   ├── home/               # 首页（7 宫格入口）
│   ├── inbound-scan/       # 扫码入库
│   ├── outbound-scan/      # 扫码出库（也用于带单出库）
│   ├── outbound-order/     # 带单出库选单
│   ├── return/             # 退库
│   └── settings/           # 设置（服务器地址/退出）
│
├── components/
│   ├── ScanInput.vue       # 扫码输入（原生input + 扫码按钮 + 查询按钮）
│   ├── MaterialCard.vue    # 物料信息卡片
│   └── FifoAlert.vue       # FIFO 预警弹窗
│
├── store/user.js           # Pinia 用户状态
└── utils/auth.js           # Token 存取（uni.storage）
```

## 6. 业务流程

### 看板号说明
所有扫码操作基于 **看板号（kanbanNo）**。看板号是入库时打印的二维码标签上的唯一编号，二维码内容格式为 `WMS-INBOUND|看板号`。扫码组件会自动去除前缀。

### 扫码入库
```
扫看板 → 查询看板信息 → 确认 → 调用入库接口 → 完成
```
- 看板状态为"已入库"时不允许重复入库
- 接口：`GET /api/inbound/scan/labels/{kanbanNo}` → `POST /api/inbound/scan/receive`

### 扫码出库（不带单）
```
扫看板 → 查询看板信息+库存+FIFO → FIFO预警弹窗 → 确认 → 调用不带单出库接口 → 自动生成出库单并完成
```
- 接口：`GET /api/outbound/scan/labels/{kanbanNo}` → `POST /api/outbound/scan/orderless-issue`
- `orderless-issue` 传物料信息，后端自动创建出库单

### 带单出库
```
选择未完成出库单 → 扫看板 → FIFO预警 → 确认 → 关联到该出库单
```
- 接口：`GET /api/outbound/orders`（选单） → `POST /api/outbound/scan/issue`（带 outboundOrderId）

### 退库
```
扫看板 → 查询退库信息 → 确认 → 库存回增+历史标记已退库
```
- 接口：`GET /api/outbound/return/labels/{kanbanNo}` → `POST /api/outbound/return`
- 退库后看板标签重置为"已入库"，同一看板可再次出库
- 整单全部退库时，出库单状态更新为"已退库"

## 7. 开发注意事项

### Vant 组件兼容性
在手机 App 的 webview 中，Vant 表单组件存在兼容问题：

| 组件 | 问题 | 解决方案 |
|------|------|----------|
| `van-field` | `v-model` 不绑定数据 | 改用原生 `<input>` |
| `van-button` | `@click` 不触发 | 改用原生 `<button>` + `@tap` |
| `van-form` | `@submit` 不触发 | 用按钮 `@click` 直接调方法 |

登录页已完全使用原生元素，其他页面通过 `easycom` + 显式 `import` 双保险导入 Vant 组件。

### easycom 配置

`pages.json` 中有 easycom 自动导入配置，使用 `van-*` 前缀的组件会自动从 `vant/es/` 目录加载：
```json
"easycom": { "custom": { "^van-(.*)": "vant/es/$1/index.mjs" } }
```

### 扫码调试
- 浏览器 H5 模式：`uni.scanCode()` 不可用，只能手动在输入框输入看板号
- Android 真机：扫码功能正常，调系统相机扫二维码

### 网络问题排查
1. 确认手机和电脑连同一 WiFi
2. `ipconfig` 确认电脑 IP，更新 `request.js` 的 `BASE_URL`
3. 电脑防火墙需放行 8080 端口
4. 后端确认已启动：浏览器访问 `http://localhost:8080/api/auth/login` 应有响应

## 8. 后端接口速查

### 手机端使用的所有接口

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/inbound/scan/labels/{kanbanNo}` | 入库扫码查询 |
| POST | `/api/inbound/scan/receive` | 入库扫码确认 |
| GET | `/api/outbound/scan/labels/{kanbanNo}` | 出库扫码查询（含FIFO） |
| POST | `/api/outbound/scan/orderless-issue` | 不带单出库 |
| POST | `/api/outbound/scan/issue` | 带单出库 |
| GET | `/api/outbound/orders` | 出库单列表 |
| GET | `/api/outbound/return/labels/{kanbanNo}` | 退库查询 |
| POST | `/api/outbound/return` | 退库确认 |

### Web 端新增接口

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/outbound/return` | 扫码退库（Web端 `ReturnScan.vue` 使用） |
| GET | `/api/outbound/return/labels/{kanbanNo}` | 退库查询 |

### Web 端新增页面

| 路由 | 页面 | 功能 |
|------|------|------|
| `/outbound/return-scan` | `views/outbound/ReturnScan.vue` | 扫码退库 |
| `/outbound/history` | `views/outbound/History.vue`（已修改） | 新增状态列 + 退库筛选 |

## 9. 数据库变更

相比原项目新增了以下字段：

```sql
-- outbound_history 表增加状态
ALTER TABLE outbound_history ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT '已出库';

-- outbound_order 表状态新增"已退库"
ALTER TABLE outbound_order MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '待出库'
  COMMENT '状态：待出库/部分完成/已完成/已退库';
```

## 10. 已知问题

- **状态多选不支持**：`GET /api/outbound/orders` 的 `status` 参数后端使用精确匹配，传 `待出库,部分完成` 查不到数据。移动端已改为前端过滤。
- **Vant 按钮在手机端偶发不触发**：登录页已改用原生 button，其他页面暂未全部替换。如果遇到点击无反应，检查是否 Vant 组件问题。
- **APK 打包需 HBuilder X 云打包**：需要注册 DCloud 账号。
