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
    created_by  VARCHAR(50)  DEFAULT 'system',
    updated_by  VARCHAR(50)  DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_user (username, password, real_name, role, status, created_by) VALUES
('admin', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '系统管理员', 'admin', 1, 'system'),
('operator', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '操作员', 'operator', 1, 'system'),
('manager', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '仓库经理', 'manager', 1, 'system');

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
    issued_by           VARCHAR(50)  DEFAULT 'system',
    status              VARCHAR(20)  NOT NULL DEFAULT '已出库',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (outbound_order_id) REFERENCES outbound_order (id)
);

-- 9. 看板标签表（简化版）
DROP TABLE IF EXISTS inbound_kanban_label;
CREATE TABLE IF NOT EXISTS inbound_kanban_label (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_order_id        BIGINT       NOT NULL,
    inbound_order_detail_id BIGINT       NOT NULL,
    doc_no                  VARCHAR(50)  NOT NULL,
    kanban_no               VARCHAR(100) NOT NULL,
    qr_payload              VARCHAR(255) NOT NULL,
    material_code           VARCHAR(50)  NOT NULL,
    material_name           VARCHAR(100) NOT NULL,
    supplier_code           VARCHAR(50)  NOT NULL,
    supplier_name           VARCHAR(100) NOT NULL,
    package_model           VARCHAR(50)  DEFAULT NULL,
    warehouse_area          VARCHAR(100) DEFAULT '默认库区',
    label_qty               INT          NOT NULL,
    package_seq             INT          NOT NULL,
    package_total           INT          NOT NULL,
    transfer_status         VARCHAR(20)  DEFAULT '不转包',
    label_status            VARCHAR(20)  NOT NULL DEFAULT '未入库',
    printed_at              TIMESTAMP    DEFAULT NULL,
    received_at             TIMESTAMP    DEFAULT NULL,
    received_by             VARCHAR(50)  DEFAULT NULL,
    created_by              VARCHAR(50)  DEFAULT 'system',
    updated_by              VARCHAR(50)  DEFAULT 'system',
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    FOREIGN KEY (inbound_order_detail_id) REFERENCES inbound_order_detail (id),
    UNIQUE (kanban_no)
);
