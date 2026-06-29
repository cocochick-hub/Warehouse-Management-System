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
    transfer_status     VARCHAR(20)  DEFAULT '不转包',
    warehouse_area      VARCHAR(100) DEFAULT '默认库区',
    created_by          VARCHAR(50)  DEFAULT 'system',
    updated_by          VARCHAR(50)  DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (material_code, supplier, warehouse_area)
);

-- 演示数据已清空，由用户自行添加



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

-- 出库单明细表和出库历史表将在后面的出库单区域统一创建
-- (不使用 DROP TABLE，数据持久保留)

-- 9. 看板标签表（简化版）

CREATE TABLE IF NOT EXISTS inbound_order (
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

CREATE TABLE IF NOT EXISTS inbound_order_detail (
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

CREATE TABLE IF NOT EXISTS inbound_kanban_label (
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
    sealed                  TINYINT      DEFAULT 0 COMMENT '是否封存：0-否 1-是',
    sealed_at               DATETIME     DEFAULT NULL COMMENT '封存时间',
    sealed_by               VARCHAR(50)  DEFAULT NULL COMMENT '封存人',
    frozen_qty              INT          DEFAULT 0 COMMENT '冻结量：封存时记录冻结数量',
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

CREATE TABLE IF NOT EXISTS inventory_stock (
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
    UNIQUE KEY uk_inventory_stock_material_supplier (material_code, supplier, warehouse_area),
    KEY idx_inventory_stock_last_inbound_at (last_inbound_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最小库存快照表';



-- ============================================================================
-- 6. 出库单表
-- ============================================================================
CREATE TABLE IF NOT EXISTS outbound_order (
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
    outbound_order_id   BIGINT       DEFAULT NULL,
    outbound_detail_id  BIGINT       DEFAULT NULL,
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



-- ============================================================================
-- 10. 库区管理表
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
-- 11. 退库功能：出库历史增加状态字段
-- ============================================================================
ALTER TABLE outbound_history
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT '已出库'
COMMENT '状态：已出库/已退库';

ALTER TABLE outbound_order
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '待出库'
COMMENT '状态：待出库/部分完成/已完成/已退库';

-- ============================================================================
-- 12. 封存功能：看板增加封存字段
-- ============================================================================
ALTER TABLE inbound_kanban_label
ADD COLUMN sealed TINYINT DEFAULT 0 COMMENT '是否封存：0-否 1-是'
AFTER received_by;

ALTER TABLE inbound_kanban_label
ADD COLUMN sealed_at DATETIME DEFAULT NULL COMMENT '封存时间'
AFTER sealed;

ALTER TABLE inbound_kanban_label
ADD COLUMN sealed_by VARCHAR(50) DEFAULT NULL COMMENT '封存人'
AFTER sealed_at;

ALTER TABLE inbound_kanban_label
ADD COLUMN frozen_qty INT DEFAULT 0 COMMENT '冻结量：封存时记录冻结数量'
AFTER sealed_by;

-- ============================================================================
-- 11. AI 预警记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS ai_alert (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_code   VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name   VARCHAR(100) NOT NULL COMMENT '物料名称',
    alert_type      VARCHAR(20)  NOT NULL COMMENT '预警类型：SHORTAGE(缺货)/DEAD_STOCK(呆滞)',
    risk_level      VARCHAR(10)  NOT NULL COMMENT '风险等级：HIGH/MEDIUM/LOW',
    current_stock   INT          NOT NULL COMMENT '当前库存',
    daily_consumption DECIMAL(10,2) DEFAULT NULL COMMENT '日均消耗量',
    estimated_days  INT          DEFAULT NULL COMMENT '预估可支撑天数',
    idle_days       INT          DEFAULT NULL COMMENT '呆滞天数',
    suggestion      TEXT         DEFAULT NULL COMMENT 'AI建议文案',
    analysis_json   TEXT         DEFAULT NULL COMMENT '完整分析数据JSON',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ai_alert_type (alert_type),
    KEY idx_ai_alert_risk (risk_level),
    KEY idx_ai_alert_material (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI预警记录表';

-- ============================================================================
-- 12. 库存表唯一键迁移：增加库区维度
-- ============================================================================
-- 删除旧的 (material_code, supplier) 唯一键，替换为 (material_code, supplier, warehouse_area)
ALTER TABLE inventory_stock DROP INDEX uk_inventory_stock_material_supplier;
ALTER TABLE inventory_stock ADD UNIQUE KEY uk_inventory_stock_material_supplier_area (material_code, supplier, warehouse_area);

-- ============================================================================
-- 13. 物料需求管理表
-- ============================================================================
CREATE TABLE IF NOT EXISTS demand_batch (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    batch_no        VARCHAR(50)  NOT NULL COMMENT '需求批次号',
    item_count      INT          NOT NULL DEFAULT 0 COMMENT '物料种类数',
    total_qty       INT          NOT NULL DEFAULT 0 COMMENT '需求总数量',
    import_type     VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '录入方式：MANUAL(手工)/EXCEL(导入)',
    created_by      VARCHAR(50)  DEFAULT 'system' COMMENT '操作人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_demand_batch_no (batch_no),
    KEY idx_demand_batch_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料需求批次表';

CREATE TABLE IF NOT EXISTS demand_detail (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    batch_id        BIGINT       NOT NULL COMMENT '需求批次ID',
    batch_no        VARCHAR(50)  NOT NULL COMMENT '需求批次号冗余',
    material_code   VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name   VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    supplier_code   VARCHAR(50)  NOT NULL COMMENT '供应商代码快照',
    supplier_name   VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    demand_qty      INT          NOT NULL COMMENT '需求数量',
    fulfilled_qty   INT          NOT NULL DEFAULT 0 COMMENT '已满足数量',
    demand_date     DATE         DEFAULT NULL COMMENT '需求日期',
    warehouse_area  VARCHAR(100) DEFAULT '默认库区' COMMENT '期望库区',
    status          VARCHAR(20)  NOT NULL DEFAULT '待出库' COMMENT '状态：待出库/部分完成/已完成',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_demand_detail_batch FOREIGN KEY (batch_id) REFERENCES demand_batch (id),
    KEY idx_demand_detail_batch_no (batch_no),
    KEY idx_demand_detail_material (material_code),
    KEY idx_demand_detail_status (status),
    KEY idx_demand_detail_demand_date (demand_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料需求明细表';

-- ============================================================================
-- 14. 高低储预警阈值配置表
-- ============================================================================
CREATE TABLE IF NOT EXISTS alert_threshold (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_code   VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name   VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    supplier        VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    low_stock_qty   INT          NOT NULL DEFAULT 0 COMMENT '低储阈值（库存低于此值预警）',
    high_stock_qty  INT          NOT NULL DEFAULT 0 COMMENT '高储阈值（库存高于此值预警）',
    created_by      VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_alert_threshold_material_supplier (material_code, supplier),
    KEY idx_alert_threshold_material (material_code),
    KEY idx_alert_threshold_supplier (supplier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高低储预警阈值配置表';

-- ============================================================================
-- 17. 转包记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS package_transfer (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    source_kanban_no    VARCHAR(100) NOT NULL COMMENT '源看板号',
    target_kanban_no    VARCHAR(100) NOT NULL COMMENT '目标看板号（新看板号）',
    transfer_qty        INT          NOT NULL COMMENT '转移数量',
    source_qty_before   INT          NOT NULL COMMENT '转移前源看板可用数量',
    source_qty_after    INT          NOT NULL COMMENT '转移后源看板可用数量',
    material_code       VARCHAR(50)  NOT NULL COMMENT '物料编码快照',
    material_name       VARCHAR(100) NOT NULL COMMENT '物料名称快照',
    supplier_name       VARCHAR(100) COMMENT '供应商名称快照',
    operator            VARCHAR(50)  COMMENT '操作人',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    source_outbound_doc_no VARCHAR(50) DEFAULT NULL COMMENT '源出库单号',
    target_inbound_doc_no  VARCHAR(50) DEFAULT NULL COMMENT '目标入库单号',
    transfer_type       VARCHAR(20)  DEFAULT NULL COMMENT '转包类型：拆包/合包',
    KEY idx_package_transfer_source (source_kanban_no),
    KEY idx_package_transfer_target (target_kanban_no),
    KEY idx_package_transfer_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转包操作记录表';

-- 兼容旧库：早期 package_transfer 表已存在时，CREATE TABLE IF NOT EXISTS 不会补新增字段。
ALTER TABLE package_transfer
ADD COLUMN source_outbound_doc_no VARCHAR(50) DEFAULT NULL COMMENT '源出库单号';

ALTER TABLE package_transfer
ADD COLUMN target_inbound_doc_no VARCHAR(50) DEFAULT NULL COMMENT '目标入库单号';

ALTER TABLE package_transfer
ADD COLUMN transfer_type VARCHAR(20) DEFAULT NULL COMMENT '转包类型：拆包/合包';

ALTER TABLE package_transfer
MODIFY COLUMN source_kanban_no VARCHAR(100) NOT NULL COMMENT '源看板号';

ALTER TABLE package_transfer
MODIFY COLUMN target_kanban_no VARCHAR(100) NOT NULL COMMENT '目标看板号（新看板号）';

-- ============================================================================
-- 18. 操作审计日志表
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    username      VARCHAR(50)   NOT NULL             COMMENT '操作人',
    action        VARCHAR(20)   NOT NULL             COMMENT '操作类型：CREATE/UPDATE/DELETE',
    target        VARCHAR(100)  NOT NULL             COMMENT '操作对象（如 InboundOrder）',
    target_id     VARCHAR(50)                        COMMENT '操作对象ID',
    detail        TEXT                               COMMENT '操作详情JSON',
    ip            VARCHAR(50)                        COMMENT '请求IP',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_audit_username (username),
    KEY idx_audit_action (action),
    KEY idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ============================================================================
-- 19. 盘点任务表
-- ============================================================================
CREATE TABLE IF NOT EXISTS inventory_check_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    task_no         VARCHAR(50)  NOT NULL UNIQUE        COMMENT '盘点单号（PD-yyyyMMdd-序号）',
    task_name       VARCHAR(100) NOT NULL               COMMENT '盘点名称（如"A区2026年6月盘点"）',
    check_type      VARCHAR(20)  NOT NULL DEFAULT '明盘' COMMENT '盘点类型：明盘/盲盘',
    status          VARCHAR(20)  NOT NULL DEFAULT '进行中' COMMENT '状态：进行中/已完成/已取消',
    warehouse_area  VARCHAR(100)                        COMMENT '盘点库区（空=全库）',
    material_code   VARCHAR(50)                         COMMENT '盘点物料（空=全部物料）',
    created_by      VARCHAR(50)                         COMMENT '创建人',
    completed_at    DATETIME                            COMMENT '完成时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点任务表';

-- ============================================================================
-- 20. 盘点明细表
-- ============================================================================
CREATE TABLE IF NOT EXISTS inventory_check_detail (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    task_id         BIGINT       NOT NULL              COMMENT '盘点任务ID',
    task_no         VARCHAR(50)  NOT NULL              COMMENT '盘点单号',
    material_code   VARCHAR(50)  NOT NULL              COMMENT '物料编码',
    material_name   VARCHAR(100) NOT NULL              COMMENT '物料名称',
    supplier        VARCHAR(100) NOT NULL              COMMENT '供应商',
    warehouse_area  VARCHAR(100)                       COMMENT '库区',
    system_qty      INT          NOT NULL DEFAULT 0    COMMENT '系统库存数量',
    actual_qty      INT                               COMMENT '实盘数量（扫码填入）',
    diff_qty        INT                               COMMENT '差异（actual - system）',
    status          VARCHAR(20)  NOT NULL DEFAULT '待盘' COMMENT '状态：待盘/已盘/已调整',
    checked_by      VARCHAR(50)                        COMMENT '盘点人',
    checked_at      DATETIME                           COMMENT '盘点时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_check_detail_task (task_id),
    KEY idx_check_detail_task_no (task_no),
    KEY idx_check_detail_material (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细表';
