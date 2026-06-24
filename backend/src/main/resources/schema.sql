-- ============================================================================
-- WMS 仓库管理系统 - H2 内存数据库测试 Schema
-- 说明：H2 MySQL 兼容模式，Spring Boot 测试环境使用
-- ============================================================================

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(50)  DEFAULT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'operator',
    status      TINYINT      NOT NULL DEFAULT 1,
    avatar      VARCHAR(255) DEFAULT NULL,
    phone       VARCHAR(30)  DEFAULT NULL,
    created_by  VARCHAR(50)  DEFAULT 'system',
    updated_by  VARCHAR(50)  DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_user (username, password, real_name, role, status, phone, created_by) VALUES
('admin', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '系统管理员', 'admin', 1, '13800000001', 'system'),
('operator', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '操作员', 'operator', 1, '13800000002', 'system'),
('manager', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '仓库经理', 'manager', 1, '13800000003', 'system');

-- 2. 供应商信息表
CREATE TABLE IF NOT EXISTS supplier_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code   VARCHAR(50)  NOT NULL,
    supplier_name   VARCHAR(100) NOT NULL,
    contact         VARCHAR(50)  DEFAULT NULL,
    phone           VARCHAR(30)  DEFAULT NULL,
    created_by      VARCHAR(50)  DEFAULT 'system',
    updated_by      VARCHAR(50)  DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (supplier_code),
    UNIQUE (supplier_name)
);

INSERT INTO supplier_info (supplier_code, supplier_name, contact, phone) VALUES
('SUP-001', '上海汽车零部件', '张工', '13800000001'),
('SUP-002', '苏州精密器具', '李经理', '13800000002'),
('SUP-003', '宁波电子模组', '王主管', '13800000003');

-- 3. 物料信息表
CREATE TABLE IF NOT EXISTS material_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_no     VARCHAR(50)  NOT NULL,
    material_name   VARCHAR(100) NOT NULL,
    material_type   VARCHAR(50)  DEFAULT NULL,
    unit            VARCHAR(20)  DEFAULT NULL,
    supplier_code   VARCHAR(50)  NOT NULL,
    supplier_name   VARCHAR(100) NOT NULL,
    created_by      VARCHAR(50)  DEFAULT 'system',
    updated_by      VARCHAR(50)  DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (material_no),
    FOREIGN KEY (supplier_code) REFERENCES supplier_info (supplier_code)
);

INSERT INTO material_info (material_no, material_name, material_type, unit, supplier_code, supplier_name) VALUES
('MAT-ENG-001', '发动机支架', '结构件', '件', 'SUP-001', '上海汽车零部件'),
('MAT-ENG-002', '减震衬套', '橡胶件', '件', 'SUP-001', '上海汽车零部件'),
('MAT-TOOL-001', '扭矩扳手', '器具', '把', 'SUP-002', '苏州精密器具'),
('MAT-TOOL-002', '定位销', '器具', '件', 'SUP-002', '苏州精密器具'),
('MAT-ELE-001', '控制器模块', '电子件', '套', 'SUP-003', '宁波电子模组'),
('MAT-ELE-002', '线束组件', '电子件', '套', 'SUP-003', '宁波电子模组');

-- 4. 包装信息表
CREATE TABLE IF NOT EXISTS packaging_info (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_no         VARCHAR(50)  NOT NULL,
    supplier_code       VARCHAR(50)  NOT NULL,
    package_model       VARCHAR(50)  DEFAULT NULL,
    package_capacity    INT          DEFAULT NULL,
    created_by          VARCHAR(50)  DEFAULT 'system',
    updated_by          VARCHAR(50)  DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (material_no, supplier_code),
    FOREIGN KEY (material_no) REFERENCES material_info (material_no),
    FOREIGN KEY (supplier_code) REFERENCES supplier_info (supplier_code)
);

INSERT INTO packaging_info (material_no, supplier_code, package_model, package_capacity) VALUES
('MAT-ENG-001', 'SUP-001', 'BX-ENG-20', 20),
('MAT-ENG-002', 'SUP-001', 'BX-RUB-50', 50),
('MAT-TOOL-001', 'SUP-002', 'BOX-TOOL-10', 10),
('MAT-TOOL-002', 'SUP-002', 'BOX-PIN-100', 100),
('MAT-ELE-001', 'SUP-003', 'BOX-ELE-8', 8),
('MAT-ELE-002', 'SUP-003', 'BOX-HAR-15', 15);

-- 5. 入库订单表
CREATE TABLE IF NOT EXISTS inbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_no            VARCHAR(50)  NOT NULL,
    supplier          VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT '未入库',
    item_count        INT          NOT NULL DEFAULT 0,
    planned_total_qty INT          NOT NULL DEFAULT 0,
    actual_total_qty  INT          NOT NULL DEFAULT 0,
    remark            VARCHAR(255) DEFAULT NULL,
    created_by        VARCHAR(50)  DEFAULT 'system',
    updated_by        VARCHAR(50)  DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (doc_no)
);

CREATE TABLE IF NOT EXISTS inbound_order_detail (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_order_id   BIGINT       NOT NULL,
    doc_no             VARCHAR(50)  NOT NULL,
    line_no            INT          NOT NULL,
    supplier_code      VARCHAR(50)  NOT NULL,
    supplier_name      VARCHAR(100) NOT NULL,
    material_code      VARCHAR(50)  NOT NULL,
    material_name      VARCHAR(100) NOT NULL,
    package_model      VARCHAR(50)  DEFAULT NULL,
    packaging_capacity INT          DEFAULT NULL,
    planned_qty        INT          NOT NULL,
    actual_qty         INT          NOT NULL DEFAULT 0,
    package_count      INT          NOT NULL DEFAULT 1,
    warehouse_area     VARCHAR(100) DEFAULT '默认库区',
    transfer_status    VARCHAR(20)  DEFAULT '不转包',
    remark             VARCHAR(255) DEFAULT NULL,
    created_by         VARCHAR(50)  DEFAULT 'system',
    updated_by         VARCHAR(50)  DEFAULT 'system',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    UNIQUE (inbound_order_id, supplier_code, material_code)
);

-- 6. 库存表
CREATE TABLE IF NOT EXISTS inventory_stock (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_code       VARCHAR(50)  NOT NULL,
    material_name       VARCHAR(100) NOT NULL,
    supplier            VARCHAR(100) NOT NULL,
    on_hand_qty         INT          NOT NULL DEFAULT 0,
    last_inbound_doc_no VARCHAR(50)  DEFAULT NULL,
    last_inbound_at     TIMESTAMP    DEFAULT NULL,
    created_by          VARCHAR(50)  DEFAULT 'system',
    updated_by          VARCHAR(50)  DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (material_code, supplier)
);

-- 7. 入库演示数据：两笔已完成入库 (为出库FIFO测试准备库存)
INSERT INTO inbound_order (id, doc_no, supplier, status, item_count, planned_total_qty, actual_total_qty, remark, created_by) VALUES
(1, 'IN20240601001', '宁波电子模组', '已完成', 2, 80, 80, '第一批入库-已完成', 'operator'),
(2, 'IN20240601002', '宁波电子模组', '已完成', 2, 70, 70, '第二批入库-已完成（用于FIFO验证）', 'operator');

INSERT INTO inbound_order_detail (id, inbound_order_id, doc_no, line_no, supplier_code, supplier_name, material_code, material_name, planned_qty, actual_qty, warehouse_area, created_by, updated_by) VALUES
(1, 1, 'IN20240601001', 1, 'SUP-003', '宁波电子模组', 'MAT-ELE-001', '控制器模块', 50, 50, '默认库区', 'operator', 'operator'),
(2, 1, 'IN20240601001', 2, 'SUP-003', '宁波电子模组', 'MAT-ELE-002', '线束组件', 30, 30, '默认库区', 'operator', 'operator'),
(3, 2, 'IN20240601002', 1, 'SUP-003', '宁波电子模组', 'MAT-ELE-001', '控制器模块', 40, 40, '默认库区', 'operator', 'operator'),
(4, 2, 'IN20240601002', 2, 'SUP-003', '宁波电子模组', 'MAT-ELE-002', '线束组件', 30, 30, '默认库区', 'operator', 'operator');

-- 库存初始化：两批入库后，MAT-ELE-001共90件(50+40), MAT-ELE-002共60件(30+30)
INSERT INTO inventory_stock (id, material_code, material_name, supplier, on_hand_qty, last_inbound_doc_no, last_inbound_at, created_by) VALUES
(1, 'MAT-ELE-001', '控制器模块', '宁波电子模组', 90, 'IN20240601002', '2026-06-10 10:00:00', 'system'),
(2, 'MAT-ELE-002', '线束组件', '宁波电子模组', 60, 'IN20240601002', '2026-06-10 10:00:00', 'system');

-- 8. 出库单表
CREATE TABLE IF NOT EXISTS outbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_no            VARCHAR(50)  NOT NULL,
    supplier          VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT '待出库',
    item_count        INT          NOT NULL DEFAULT 0,
    planned_total_qty INT          NOT NULL DEFAULT 0,
    actual_total_qty  INT          NOT NULL DEFAULT 0,
    remark            VARCHAR(255) DEFAULT NULL,
    created_by        VARCHAR(50)  DEFAULT 'system',
    updated_by        VARCHAR(50)  DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (doc_no)
);

-- 出库单明细表和出库历史表将在后面的出库单区域统一重建
DROP TABLE IF EXISTS outbound_order_detail;
DROP TABLE IF EXISTS outbound_history;

-- 9. 看板标签表（简化版）
DROP TABLE IF EXISTS inbound_kanban_label;
DROP TABLE IF EXISTS inbound_order_detail;
DROP TABLE IF EXISTS inventory_stock;
DROP TABLE IF EXISTS inbound_order;

CREATE TABLE inbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    doc_no            VARCHAR(50)  NOT NULL COMMENT '入库单号',
    supplier          VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    status            VARCHAR(20)  NOT NULL DEFAULT '未入库' COMMENT '状态：未入库/部分完成/已完成',
    item_count        INT          NOT NULL DEFAULT 0 COMMENT '明细条数',
    planned_total_qty INT          NOT NULL DEFAULT 0 COMMENT '计划总数',
    actual_total_qty  INT          NOT NULL DEFAULT 0 COMMENT '实收总数',
    transfer_status   VARCHAR(20)  DEFAULT '不转包' COMMENT '转包状态：不转包/转包',
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
    supplier_code      VARCHAR(50)  NOT NULL COMMENT '供应商代码快照',
    supplier_name      VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    material_code      VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name      VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    package_model      VARCHAR(50)  DEFAULT NULL COMMENT '包装型号/器具型号',
    packaging_capacity INT          DEFAULT NULL COMMENT '包装容量',
    planned_qty        INT          NOT NULL COMMENT '计划入库数量',
    actual_qty         INT          NOT NULL DEFAULT 0 COMMENT '累计实际入库数量',
    package_count      INT          NOT NULL DEFAULT 1 COMMENT '预计包数',
    warehouse_area     VARCHAR(100) DEFAULT '默认库区' COMMENT '库区',
    transfer_status    VARCHAR(20)  DEFAULT '不转包' COMMENT '转包状态',
    remark             VARCHAR(255) DEFAULT NULL COMMENT '明细备注',
    created_by         VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by         VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_inbound_order_detail_order_id FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    UNIQUE KEY uk_inbound_order_detail_order_supplier_material (inbound_order_id, supplier_code, material_code),
    KEY idx_inbound_order_detail_doc_no (doc_no),
    KEY idx_inbound_order_detail_material_code (material_code),
    KEY idx_inbound_order_detail_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

CREATE TABLE inbound_kanban_label (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    inbound_order_id        BIGINT       NOT NULL COMMENT '入库单ID',
    inbound_order_detail_id BIGINT       NOT NULL COMMENT '入库明细ID',
    doc_no                  VARCHAR(50)  NOT NULL COMMENT '入库单号',
    kanban_no               VARCHAR(100) NOT NULL COMMENT '唯一看板号',
    qr_payload              VARCHAR(255) NOT NULL COMMENT '二维码内容',
    material_code           VARCHAR(50)  NOT NULL COMMENT '物料号快照',
    material_name           VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    supplier_code           VARCHAR(50)  NOT NULL COMMENT '供应商代码快照',
    supplier_name           VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    package_model           VARCHAR(50)  DEFAULT NULL COMMENT '器具型号',
    warehouse_area          VARCHAR(100) DEFAULT '默认库区' COMMENT '库区',
    label_qty               INT          NOT NULL COMMENT '本看板数量',
    package_seq             INT          NOT NULL COMMENT '当前第几包',
    package_total           INT          NOT NULL COMMENT '共几包',
    transfer_status         VARCHAR(20)  DEFAULT '不转包' COMMENT '转包状态',
    label_status            VARCHAR(20)  NOT NULL DEFAULT '未入库' COMMENT '看板状态：未入库/已入库/作废',
    printed_at              DATETIME     DEFAULT NULL COMMENT '最近打印时间',
    received_at             DATETIME     DEFAULT NULL COMMENT '扫码入库时间',
    received_by             VARCHAR(50)  DEFAULT NULL COMMENT '扫码入库人',
    created_by              VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by              VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_inbound_kanban_label_order_id FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    CONSTRAINT fk_inbound_kanban_label_detail_id FOREIGN KEY (inbound_order_detail_id) REFERENCES inbound_order_detail (id),
    UNIQUE KEY uk_inbound_kanban_label_no (kanban_no),
    KEY idx_inbound_kanban_label_doc_no (doc_no),
    KEY idx_inbound_kanban_label_status (label_status),
    KEY idx_inbound_kanban_label_detail_id (inbound_order_detail_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库二维码看板表';

CREATE TABLE inventory_stock (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_code       VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name       VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    supplier            VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    on_hand_qty         INT          NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    last_inbound_doc_no VARCHAR(50)  DEFAULT NULL COMMENT '最近入库单号',
    last_inbound_at     DATETIME     DEFAULT NULL COMMENT '最近入库时间',
    transfer_status     VARCHAR(20)  DEFAULT '不转包' COMMENT '转包状态：不转包/转包',
    warehouse_area      VARCHAR(100) DEFAULT '默认库区' COMMENT '库区',
    created_by          VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by          VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_inventory_stock_material_supplier (material_code, supplier),
    KEY idx_inventory_stock_last_inbound_at (last_inbound_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最小库存快照表';

-- 5. 入库演示数据
INSERT INTO inbound_order (
    id, doc_no, supplier, status, item_count, planned_total_qty, actual_total_qty, transfer_status, remark, created_by, updated_by
) VALUES
    (1, 'IN20240601001', '多供应商', '未入库', 2, 180, 0, '不转包', '演示未入库单据', 'operator', 'operator'),
    (2, 'IN20240601002', '长春一汽配套', '部分完成', 2, 180, 70, '转包', '演示部分完成单据', 'operator', 'operator');

INSERT INTO inbound_order_detail (
    id, inbound_order_id, doc_no, line_no, supplier_code, supplier_name, material_code, material_name, package_model, packaging_capacity, planned_qty, actual_qty, package_count, warehouse_area, transfer_status, remark, created_by, updated_by
) VALUES
    (1, 1, 'IN20240601001', 1, 'SUP-001', '上海汽车零部件', 'MAT-ENG-001', '发动机支架', 'BX-ENG-20', 20, 100, 0, 5, '默认库区', '不转包', '待入库明细', 'operator', 'operator'),
    (2, 1, 'IN20240601001', 2, 'SUP-002', '苏州精密器具', 'MAT-TOOL-002', '定位销', 'BOX-PIN-100', 100, 80, 0, 1, '默认库区', '不转包', '待入库明细', 'operator', 'operator'),
    (3, 2, 'IN20240601002', 1, 'SUP-003', '宁波电子模组', 'MAT-ELE-001', '控制器模块', 'BOX-ELE-8', 8, 60, 30, 8, '默认库区', '不转包', '已部分入库', 'operator', 'operator'),
    (4, 2, 'IN20240601002', 2, 'SUP-003', '宁波电子模组', 'MAT-ELE-002', '线束组件', 'BOX-HAR-15', 15, 120, 40, 8, '默认库区', '不转包', '已部分入库', 'operator', 'operator');

INSERT INTO inventory_stock (
    id, material_code, material_name, supplier, on_hand_qty, last_inbound_doc_no, last_inbound_at, transfer_status, warehouse_area, created_by, updated_by
) VALUES
    (1, 'MAT-ELE-001', '控制器模块', '宁波电子模组', 30, 'IN20240601002', '2026-06-07 10:00:00', '转包', '默认库区', 'system', 'system'),
    (2, 'MAT-ELE-002', '线束组件', '宁波电子模组', 40, 'IN20240601002', '2026-06-07 10:00:00', '转包', '默认库区', 'system', 'system');

-- ============================================================================
-- 6. 出库单表
-- ============================================================================
DROP TABLE IF EXISTS outbound_order_detail;
DROP TABLE IF EXISTS outbound_order;
CREATE TABLE outbound_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    doc_no          VARCHAR(50)  NOT NULL UNIQUE       COMMENT '出库单号',
    supplier        VARCHAR(100) NOT NULL              COMMENT '供应商',
    status          VARCHAR(20)  NOT NULL DEFAULT '待出库' COMMENT '状态：待出库/部分完成/已完成',
    item_count      INT          NOT NULL DEFAULT 0    COMMENT '明细条数',
    planned_total_qty INT       NOT NULL DEFAULT 0    COMMENT '计划出库总数',
    actual_total_qty INT        NOT NULL DEFAULT 0    COMMENT '实际出库总数',
    outbound_type   VARCHAR(20) NOT NULL DEFAULT '带单出库' COMMENT '出库方式(带单出库/不带单出库)',
    remark          VARCHAR(255) DEFAULT NULL          COMMENT '备注',
    created_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '更新人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_doc_no (doc_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- 出库单明细表
CREATE TABLE IF NOT EXISTS outbound_order_detail (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_order_id   BIGINT       NOT NULL,
    doc_no              VARCHAR(50)  NOT NULL,
    line_no             INT          NOT NULL,
    supplier_code       VARCHAR(50)  NOT NULL,
    supplier_name       VARCHAR(100) NOT NULL,
    material_code       VARCHAR(50)  NOT NULL,
    material_name       VARCHAR(100) NOT NULL,
    planned_qty         INT          NOT NULL,
    actual_qty          INT          NOT NULL DEFAULT 0,
    warehouse_area      VARCHAR(100) DEFAULT '默认库区',
    remark              VARCHAR(255) DEFAULT NULL,
    created_by          VARCHAR(50)  DEFAULT 'system',
    updated_by          VARCHAR(50)  DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (outbound_order_id) REFERENCES outbound_order (id),
    UNIQUE (outbound_order_id, supplier_code, material_code)
);

-- 出库历史表
CREATE TABLE IF NOT EXISTS outbound_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_order_id   BIGINT       NOT NULL,
    outbound_detail_id  BIGINT       NOT NULL,
    doc_no              VARCHAR(50)  NOT NULL,
    material_code       VARCHAR(50)  NOT NULL,
    material_name       VARCHAR(100) NOT NULL,
    supplier_name       VARCHAR(100) NOT NULL,
    issue_qty           INT          NOT NULL,
    source_inbound_doc  VARCHAR(50)  DEFAULT NULL,
    source_detail_id    BIGINT       DEFAULT NULL,
    warehouse_area      VARCHAR(100) DEFAULT '默认库区',
    issued_by           VARCHAR(50)  DEFAULT NULL,
    issued_at           TIMESTAMP    DEFAULT NULL,
    remark              VARCHAR(255) DEFAULT NULL,
    created_by          VARCHAR(50)  DEFAULT 'system',
    updated_by          VARCHAR(50)  DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (outbound_order_id) REFERENCES outbound_order (id),
    FOREIGN KEY (outbound_detail_id) REFERENCES outbound_order_detail (id)
);

-- 7. 出库单演示数据
INSERT INTO outbound_order (doc_no, supplier, status, created_by) VALUES
('OUT20240601001', '广州本田', '待出库', 'operator'),
('OUT20240601002', '武汉东风', '待出库', 'operator')
ON DUPLICATE KEY UPDATE doc_no = VALUES(doc_no);

-- ============================================================================
-- 8. AI 预警记录表
-- ============================================================================
DROP TABLE IF EXISTS ai_alert;
CREATE TABLE ai_alert (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_code       VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name       VARCHAR(100) NOT NULL COMMENT '物料名称',
    alert_type          VARCHAR(20)  NOT NULL COMMENT '预警类型: SHORTAGE(缺货) / DEAD_STOCK(呆滞)',
    risk_level          VARCHAR(10)  NOT NULL COMMENT '风险等级: HIGH / MEDIUM / LOW',
    current_stock       INT          NOT NULL COMMENT '当前库存',
    daily_consumption   DECIMAL(10,2) DEFAULT NULL COMMENT '日均消耗量',
    estimated_days      INT          DEFAULT NULL COMMENT '预估可支撑天数',
    idle_days           INT          DEFAULT NULL COMMENT '呆滞天数',
    suggestion          TEXT         DEFAULT NULL COMMENT 'AI建议文案',
    analysis_json       TEXT         DEFAULT NULL COMMENT '完整分析JSON',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ai_alert_type (alert_type),
    KEY idx_ai_alert_risk (risk_level),
    KEY idx_ai_alert_material (material_code),
    KEY idx_ai_alert_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI预警记录表';

-- ============================================================================
-- 9. 库区管理表
-- ============================================================================
CREATE TABLE IF NOT EXISTS warehouse_area (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    area_code       VARCHAR(50)  NOT NULL COMMENT '库区代码',
    area_name       VARCHAR(100) NOT NULL COMMENT '库区名称',
    sort_order      INT          DEFAULT 0 COMMENT '排序号',
    description     VARCHAR(255) DEFAULT NULL COMMENT '描述说明',
    created_by      VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_warehouse_area_code (area_code),
    KEY idx_warehouse_area_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库区管理表';

INSERT INTO warehouse_area (area_code, area_name, sort_order, description) VALUES
('WA-DEFAULT', '默认库区', 1, '系统默认库区'),
('WA-AREA-01', '库区1', 2, '库区1'),
('WA-AREA-02', '库区2', 3, '库区2')
ON DUPLICATE KEY UPDATE area_name = VALUES(area_name);

-- ============================================================================
-- 10. 退库功能：出库历史增加状态字段
-- ============================================================================
ALTER TABLE outbound_history
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT '已出库'
COMMENT '状态：已出库/已退库';

ALTER TABLE outbound_order
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '待出库'
COMMENT '状态：待出库/部分完成/已完成/已退库';
