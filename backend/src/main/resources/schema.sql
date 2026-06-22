-- ============================================================================
-- WMS 仓库管理系统 - 数据库初始化脚本
-- 说明：启动时会自动执行此脚本创建表结构并插入初始数据
-- ============================================================================

-- 1. 系统用户表（登录认证）
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL UNIQUE       COMMENT '用户名（登录账号）',
    password    VARCHAR(255) NOT NULL              COMMENT '密码（BCrypt 加密）',
    real_name   VARCHAR(50)  DEFAULT NULL          COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'operator' COMMENT '角色：admin-管理员 / operator-操作员 / manager-经理',
    status      TINYINT      NOT NULL DEFAULT 1    COMMENT '状态：1-启用 / 0-禁用',
    avatar      VARCHAR(255) DEFAULT NULL          COMMENT '头像 URL',
    created_by  VARCHAR(50)  DEFAULT 'system'     COMMENT '创建人',
    updated_by  VARCHAR(50)  DEFAULT 'system'     COMMENT '更新人',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 插入初始管理员账号（密码为 admin123 的 BCrypt 加密值）
-- 密码生成：BCrypt(admin123)
-- 如需生成新密码，可使用在线 BCrypt 工具或 Java 的 BCryptPasswordEncoder
INSERT INTO sys_user (username, password, real_name, role, status, created_by) VALUES
('admin', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '系统管理员', 'admin', 1, 'system'),
('operator', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '操作员', 'operator', 1, 'system'),
('manager', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '仓库经理', 'manager', 1, 'system')
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- ============================================================================
-- 3. 基础信息表
-- ============================================================================
DROP TABLE IF EXISTS packaging_info;
DROP TABLE IF EXISTS material_info;
DROP TABLE IF EXISTS supplier_info;

CREATE TABLE supplier_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    supplier_code   VARCHAR(50)  NOT NULL COMMENT '供应商代码',
    supplier_name   VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact         VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    phone           VARCHAR(30)  DEFAULT NULL COMMENT '联系电话',
    created_by      VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_supplier_info_code (supplier_code),
    UNIQUE KEY uk_supplier_info_name (supplier_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商信息表';

CREATE TABLE material_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_no     VARCHAR(50)  NOT NULL COMMENT '物料号',
    material_name   VARCHAR(100) NOT NULL COMMENT '物料名称',
    material_type   VARCHAR(50)  DEFAULT NULL COMMENT '物料类型',
    unit            VARCHAR(20)  DEFAULT NULL COMMENT '单位',
    supplier_code   VARCHAR(50)  NOT NULL COMMENT '供应商代码',
    supplier_name   VARCHAR(100) NOT NULL COMMENT '供应商名称快照',
    created_by      VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_material_info_no (material_no),
    KEY idx_material_info_supplier_code (supplier_code),
    CONSTRAINT fk_material_info_supplier_code FOREIGN KEY (supplier_code) REFERENCES supplier_info (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料信息表';

CREATE TABLE packaging_info (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_no         VARCHAR(50)  NOT NULL COMMENT '物料号',
    supplier_code       VARCHAR(50)  NOT NULL COMMENT '供应商代码',
    package_model       VARCHAR(50)  DEFAULT NULL COMMENT '包装型号',
    package_capacity    INT          DEFAULT NULL COMMENT '包装容量',
    created_by          VARCHAR(50)  DEFAULT 'system' COMMENT '创建人',
    updated_by          VARCHAR(50)  DEFAULT 'system' COMMENT '更新人',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_packaging_info_material_supplier (material_no, supplier_code),
    KEY idx_packaging_info_supplier_code (supplier_code),
    CONSTRAINT fk_packaging_info_material_no FOREIGN KEY (material_no) REFERENCES material_info (material_no),
    CONSTRAINT fk_packaging_info_supplier_code FOREIGN KEY (supplier_code) REFERENCES supplier_info (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包装信息表';

INSERT INTO supplier_info (supplier_code, supplier_name, contact, phone, created_by, updated_by) VALUES
('SUP-001', '上海汽车零部件', '张工', '13800000001', 'system', 'system'),
('SUP-002', '苏州精密器具', '李经理', '13800000002', 'system', 'system'),
('SUP-003', '宁波电子模组', '王主管', '13800000003', 'system', 'system');

INSERT INTO material_info (material_no, material_name, material_type, unit, supplier_code, supplier_name, created_by, updated_by) VALUES
('MAT-ENG-001', '发动机支架', '结构件', '件', 'SUP-001', '上海汽车零部件', 'system', 'system'),
('MAT-ENG-002', '减震衬套', '橡胶件', '件', 'SUP-001', '上海汽车零部件', 'system', 'system'),
('MAT-TOOL-001', '扭矩扳手', '器具', '把', 'SUP-002', '苏州精密器具', 'system', 'system'),
('MAT-TOOL-002', '定位销', '器具', '件', 'SUP-002', '苏州精密器具', 'system', 'system'),
('MAT-ELE-001', '控制器模块', '电子件', '套', 'SUP-003', '宁波电子模组', 'system', 'system'),
('MAT-ELE-002', '线束组件', '电子件', '套', 'SUP-003', '宁波电子模组', 'system', 'system');

INSERT INTO packaging_info (material_no, supplier_code, package_model, package_capacity, created_by, updated_by) VALUES
('MAT-ENG-001', 'SUP-001', 'BX-ENG-20', 20, 'system', 'system'),
('MAT-ENG-002', 'SUP-001', 'BX-RUB-50', 50, 'system', 'system'),
('MAT-TOOL-001', 'SUP-002', 'BOX-TOOL-10', 10, 'system', 'system'),
('MAT-TOOL-002', 'SUP-002', 'BOX-PIN-100', 100, 'system', 'system'),
('MAT-ELE-001', 'SUP-003', 'BOX-ELE-8', 8, 'system', 'system'),
('MAT-ELE-002', 'SUP-003', 'BOX-HAR-15', 15, 'system', 'system');

-- ============================================================================
-- 4. 入库相关表
-- ============================================================================
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
DROP TABLE IF EXISTS outbound_order;
CREATE TABLE outbound_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    doc_no          VARCHAR(50)  NOT NULL UNIQUE       COMMENT '出库单号',
    supplier        VARCHAR(100) NOT NULL              COMMENT '供应商',
    status          VARCHAR(20)  NOT NULL DEFAULT '待出库' COMMENT '状态：待出库/部分完成/已完成',
    created_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '更新人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_doc_no (doc_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- 7. 出库单演示数据
INSERT INTO outbound_order (doc_no, supplier, status, created_by) VALUES
('OUT20240601001', '广州本田', '待出库', 'operator'),
('OUT20240601002', '武汉东风', '待出库', 'operator')
ON DUPLICATE KEY UPDATE doc_no = VALUES(doc_no);
