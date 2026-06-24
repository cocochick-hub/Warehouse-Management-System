﻿-- ============================================================================
-- WMS 浠撳簱绠＄悊绯荤粺 - H2 鍐呭瓨鏁版嵁搴撴祴璇?Schema
-- 璇存槑锛欻2 MySQL 鍏煎妯″紡锛孲pring Boot 娴嬭瘯鐜浣跨敤
-- ============================================================================

-- 1. 绯荤粺鐢ㄦ埛琛?
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
('admin', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '绯荤粺绠＄悊鍛?, 'admin', 1, '13800000001', 'system'),
('operator', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '鎿嶄綔鍛?, 'operator', 1, '13800000002', 'system'),
('manager', '$2a$10$Dn9GTyqZzZEK3eIfUa5Fsek4UOKRSNi9amDyMXp4sosnOU3MqaKBK', '浠撳簱缁忕悊', 'manager', 1, '13800000003', 'system');

-- 2. 渚涘簲鍟嗕俊鎭〃
CREATE TABLE IF NOT EXISTS supplier_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code   VARCHAR(50)  NOT NULL,
    supplier_name   VARCHAR(100) NOT NULL,
    contact         VARCHAR(50)  DEFAULT NULL,
    
    created_by      VARCHAR(50)  DEFAULT 'system',
    updated_by      VARCHAR(50)  DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (supplier_code),
    UNIQUE (supplier_name)
);

INSERT INTO supplier_info (supplier_code, supplier_name, contact, phone) VALUES
('SUP-001', '涓婃捣姹借溅闆堕儴浠?, '寮犲伐', '13800000001'),
('SUP-002', '鑻忓窞绮惧瘑鍣ㄥ叿', '鏉庣粡鐞?, '13800000002'),
('SUP-003', '瀹佹尝鐢靛瓙妯＄粍', '鐜嬩富绠?, '13800000003');

-- 3. 鐗╂枡淇℃伅琛?
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
('MAT-ENG-001', '鍙戝姩鏈烘敮鏋?, '缁撴瀯浠?, '浠?, 'SUP-001', '涓婃捣姹借溅闆堕儴浠?),
('MAT-ENG-002', '鍑忛渿琛', '姗¤兌浠?, '浠?, 'SUP-001', '涓婃捣姹借溅闆堕儴浠?),
('MAT-TOOL-001', '鎵煩鎵虫墜', '鍣ㄥ叿', '鎶?, 'SUP-002', '鑻忓窞绮惧瘑鍣ㄥ叿'),
('MAT-TOOL-002', '瀹氫綅閿€', '鍣ㄥ叿', '浠?, 'SUP-002', '鑻忓窞绮惧瘑鍣ㄥ叿'),
('MAT-ELE-001', '鎺у埗鍣ㄦā鍧?, '鐢靛瓙浠?, '濂?, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍'),
('MAT-ELE-002', '绾挎潫缁勪欢', '鐢靛瓙浠?, '濂?, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍');

-- 4. 鍖呰淇℃伅琛?
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

-- 5. 鍏ュ簱璁㈠崟琛?
CREATE TABLE IF NOT EXISTS inbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_no            VARCHAR(50)  NOT NULL,
    supplier          VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT '鏈叆搴?,
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
    warehouse_area     VARCHAR(100) DEFAULT '榛樿搴撳尯',
    transfer_status    VARCHAR(20)  DEFAULT '涓嶈浆鍖?,
    remark             VARCHAR(255) DEFAULT NULL,
    created_by         VARCHAR(50)  DEFAULT 'system',
    updated_by         VARCHAR(50)  DEFAULT 'system',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    UNIQUE (inbound_order_id, supplier_code, material_code)
);

-- 6. 搴撳瓨琛?
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

-- 7. 鍏ュ簱婕旂ず鏁版嵁锛氫袱绗斿凡瀹屾垚鍏ュ簱 (涓哄嚭搴揊IFO娴嬭瘯鍑嗗搴撳瓨)
INSERT INTO inbound_order (id, doc_no, supplier, status, item_count, planned_total_qty, actual_total_qty, remark, created_by) VALUES
(1, 'IN20240601001', '瀹佹尝鐢靛瓙妯＄粍', '宸插畬鎴?, 2, 80, 80, '绗竴鎵瑰叆搴?宸插畬鎴?, 'operator'),
(2, 'IN20240601002', '瀹佹尝鐢靛瓙妯＄粍', '宸插畬鎴?, 2, 70, 70, '绗簩鎵瑰叆搴?宸插畬鎴愶紙鐢ㄤ簬FIFO楠岃瘉锛?, 'operator');

INSERT INTO inbound_order_detail (id, inbound_order_id, doc_no, line_no, supplier_code, supplier_name, material_code, material_name, planned_qty, actual_qty, warehouse_area, created_by, updated_by) VALUES
(1, 1, 'IN20240601001', 1, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍', 'MAT-ELE-001', '鎺у埗鍣ㄦā鍧?, 50, 50, '榛樿搴撳尯', 'operator', 'operator'),
(2, 1, 'IN20240601001', 2, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍', 'MAT-ELE-002', '绾挎潫缁勪欢', 30, 30, '榛樿搴撳尯', 'operator', 'operator'),
(3, 2, 'IN20240601002', 1, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍', 'MAT-ELE-001', '鎺у埗鍣ㄦā鍧?, 40, 40, '榛樿搴撳尯', 'operator', 'operator'),
(4, 2, 'IN20240601002', 2, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍', 'MAT-ELE-002', '绾挎潫缁勪欢', 30, 30, '榛樿搴撳尯', 'operator', 'operator');

-- 搴撳瓨鍒濆鍖栵細涓ゆ壒鍏ュ簱鍚庯紝MAT-ELE-001鍏?0浠?50+40), MAT-ELE-002鍏?0浠?30+30)
INSERT INTO inventory_stock (id, material_code, material_name, supplier, on_hand_qty, last_inbound_doc_no, last_inbound_at, created_by) VALUES
(1, 'MAT-ELE-001', '鎺у埗鍣ㄦā鍧?, '瀹佹尝鐢靛瓙妯＄粍', 90, 'IN20240601002', '2026-06-10 10:00:00', 'system'),
(2, 'MAT-ELE-002', '绾挎潫缁勪欢', '瀹佹尝鐢靛瓙妯＄粍', 60, 'IN20240601002', '2026-06-10 10:00:00', 'system');

-- 8. 鍑哄簱鍗曡〃
CREATE TABLE IF NOT EXISTS outbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_no            VARCHAR(50)  NOT NULL,
    supplier          VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT '寰呭嚭搴?,
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

-- 鍑哄簱鍗曟槑缁嗚〃鍜屽嚭搴撳巻鍙茶〃灏嗗湪鍚庨潰鐨勫嚭搴撳崟鍖哄煙缁熶竴閲嶅缓
DROP TABLE IF EXISTS outbound_order_detail;
DROP TABLE IF EXISTS outbound_history;

-- 9. 鐪嬫澘鏍囩琛紙绠€鍖栫増锛?
DROP TABLE IF EXISTS inbound_kanban_label;
DROP TABLE IF EXISTS inbound_order_detail;
DROP TABLE IF EXISTS inventory_stock;
DROP TABLE IF EXISTS inbound_order;

CREATE TABLE inbound_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓婚敭ID',
    doc_no            VARCHAR(50)  NOT NULL COMMENT '鍏ュ簱鍗曞彿',
    supplier          VARCHAR(100) NOT NULL COMMENT '渚涘簲鍟嗗悕绉板揩鐓?,
    status            VARCHAR(20)  NOT NULL DEFAULT '鏈叆搴? COMMENT '鐘舵€侊細鏈叆搴?閮ㄥ垎瀹屾垚/宸插畬鎴?,
    item_count        INT          NOT NULL DEFAULT 0 COMMENT '鏄庣粏鏉℃暟',
    planned_total_qty INT          NOT NULL DEFAULT 0 COMMENT '璁″垝鎬绘暟',
    actual_total_qty  INT          NOT NULL DEFAULT 0 COMMENT '瀹炴敹鎬绘暟',
    transfer_status   VARCHAR(20)  DEFAULT '涓嶈浆鍖? COMMENT '杞寘鐘舵€侊細涓嶈浆鍖?杞寘',
    remark            VARCHAR(255) DEFAULT NULL COMMENT '澶囨敞',
    created_by        VARCHAR(50)  DEFAULT 'system' COMMENT '鍒涘缓浜?,
    updated_by        VARCHAR(50)  DEFAULT 'system' COMMENT '鏇存柊浜?,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_inbound_order_doc_no (doc_no),
    KEY idx_inbound_order_status (status),
    KEY idx_inbound_order_supplier (supplier),
    KEY idx_inbound_order_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍏ュ簱鍗曞ご琛?;

CREATE TABLE inbound_order_detail (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓婚敭ID',
    inbound_order_id   BIGINT       NOT NULL COMMENT '鍏ュ簱鍗旾D',
    doc_no             VARCHAR(50)  NOT NULL COMMENT '鍏ュ簱鍗曞彿鍐椾綑',
    line_no            INT          NOT NULL COMMENT '琛屽彿',
    supplier_code      VARCHAR(50)  NOT NULL COMMENT '渚涘簲鍟嗕唬鐮佸揩鐓?,
    supplier_name      VARCHAR(100) NOT NULL COMMENT '渚涘簲鍟嗗悕绉板揩鐓?,
    material_code      VARCHAR(50)  NOT NULL COMMENT '鐗╂枡鍙?,
    material_name      VARCHAR(100) NOT NULL COMMENT '鐗╂枡鍚嶇О蹇収',
    package_model      VARCHAR(50)  DEFAULT NULL COMMENT '鍖呰鍨嬪彿/鍣ㄥ叿鍨嬪彿',
    packaging_capacity INT          DEFAULT NULL COMMENT '鍖呰瀹归噺',
    planned_qty        INT          NOT NULL COMMENT '璁″垝鍏ュ簱鏁伴噺',
    actual_qty         INT          NOT NULL DEFAULT 0 COMMENT '绱瀹為檯鍏ュ簱鏁伴噺',
    package_count      INT          NOT NULL DEFAULT 1 COMMENT '棰勮鍖呮暟',
    warehouse_area     VARCHAR(100) DEFAULT '榛樿搴撳尯' COMMENT '搴撳尯',
    transfer_status    VARCHAR(20)  DEFAULT '涓嶈浆鍖? COMMENT '杞寘鐘舵€?,
    remark             VARCHAR(255) DEFAULT NULL COMMENT '鏄庣粏澶囨敞',
    created_by         VARCHAR(50)  DEFAULT 'system' COMMENT '鍒涘缓浜?,
    updated_by         VARCHAR(50)  DEFAULT 'system' COMMENT '鏇存柊浜?,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    CONSTRAINT fk_inbound_order_detail_order_id FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    UNIQUE KEY uk_inbound_order_detail_order_supplier_material (inbound_order_id, supplier_code, material_code),
    KEY idx_inbound_order_detail_doc_no (doc_no),
    KEY idx_inbound_order_detail_material_code (material_code),
    KEY idx_inbound_order_detail_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍏ュ簱鍗曟槑缁嗚〃';

CREATE TABLE inbound_kanban_label (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓婚敭ID',
    inbound_order_id        BIGINT       NOT NULL COMMENT '鍏ュ簱鍗旾D',
    inbound_order_detail_id BIGINT       NOT NULL COMMENT '鍏ュ簱鏄庣粏ID',
    doc_no                  VARCHAR(50)  NOT NULL COMMENT '鍏ュ簱鍗曞彿',
    kanban_no               VARCHAR(100) NOT NULL COMMENT '鍞竴鐪嬫澘鍙?,
    qr_payload              VARCHAR(255) NOT NULL COMMENT '浜岀淮鐮佸唴瀹?,
    material_code           VARCHAR(50)  NOT NULL COMMENT '鐗╂枡鍙峰揩鐓?,
    material_name           VARCHAR(100) NOT NULL COMMENT '鐗╂枡鍚嶇О蹇収',
    supplier_code           VARCHAR(50)  NOT NULL COMMENT '渚涘簲鍟嗕唬鐮佸揩鐓?,
    supplier_name           VARCHAR(100) NOT NULL COMMENT '渚涘簲鍟嗗悕绉板揩鐓?,
    package_model           VARCHAR(50)  DEFAULT NULL COMMENT '鍣ㄥ叿鍨嬪彿',
    warehouse_area          VARCHAR(100) DEFAULT '榛樿搴撳尯' COMMENT '搴撳尯',
    label_qty               INT          NOT NULL COMMENT '鏈湅鏉挎暟閲?,
    package_seq             INT          NOT NULL COMMENT '褰撳墠绗嚑鍖?,
    package_total           INT          NOT NULL COMMENT '鍏卞嚑鍖?,
    transfer_status         VARCHAR(20)  DEFAULT '涓嶈浆鍖? COMMENT '杞寘鐘舵€?,
    label_status            VARCHAR(20)  NOT NULL DEFAULT '鏈叆搴? COMMENT '鐪嬫澘鐘舵€侊細鏈叆搴?宸插叆搴?浣滃簾',
    printed_at              DATETIME     DEFAULT NULL COMMENT '鏈€杩戞墦鍗版椂闂?,
    received_at             DATETIME     DEFAULT NULL COMMENT '鎵爜鍏ュ簱鏃堕棿',
    received_by             VARCHAR(50)  DEFAULT NULL COMMENT '鎵爜鍏ュ簱浜?,
    created_by              VARCHAR(50)  DEFAULT 'system' COMMENT '鍒涘缓浜?,
    updated_by              VARCHAR(50)  DEFAULT 'system' COMMENT '鏇存柊浜?,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    CONSTRAINT fk_inbound_kanban_label_order_id FOREIGN KEY (inbound_order_id) REFERENCES inbound_order (id),
    CONSTRAINT fk_inbound_kanban_label_detail_id FOREIGN KEY (inbound_order_detail_id) REFERENCES inbound_order_detail (id),
    UNIQUE KEY uk_inbound_kanban_label_no (kanban_no),
    KEY idx_inbound_kanban_label_doc_no (doc_no),
    KEY idx_inbound_kanban_label_status (label_status),
    KEY idx_inbound_kanban_label_detail_id (inbound_order_detail_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍏ュ簱浜岀淮鐮佺湅鏉胯〃';

CREATE TABLE inventory_stock (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓婚敭ID',
    material_code       VARCHAR(50)  NOT NULL COMMENT '鐗╂枡鍙?,
    material_name       VARCHAR(100) NOT NULL COMMENT '鐗╂枡鍚嶇О蹇収',
    supplier            VARCHAR(100) NOT NULL COMMENT '渚涘簲鍟嗗悕绉板揩鐓?,
    on_hand_qty         INT          NOT NULL DEFAULT 0 COMMENT '褰撳墠搴撳瓨鏁伴噺',
    last_inbound_doc_no VARCHAR(50)  DEFAULT NULL COMMENT '鏈€杩戝叆搴撳崟鍙?,
    last_inbound_at     DATETIME     DEFAULT NULL COMMENT '鏈€杩戝叆搴撴椂闂?,
    transfer_status     VARCHAR(20)  DEFAULT '涓嶈浆鍖? COMMENT '杞寘鐘舵€侊細涓嶈浆鍖?杞寘',
    warehouse_area      VARCHAR(100) DEFAULT '榛樿搴撳尯' COMMENT '搴撳尯',
    created_by          VARCHAR(50)  DEFAULT 'system' COMMENT '鍒涘缓浜?,
    updated_by          VARCHAR(50)  DEFAULT 'system' COMMENT '鏇存柊浜?,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_inventory_stock_material_supplier (material_code, supplier),
    KEY idx_inventory_stock_last_inbound_at (last_inbound_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鏈€灏忓簱瀛樺揩鐓ц〃';

-- 5. 鍏ュ簱婕旂ず鏁版嵁
INSERT INTO inbound_order (
    id, doc_no, supplier, status, item_count, planned_total_qty, actual_total_qty, transfer_status, remark, created_by, updated_by
) VALUES
    (1, 'IN20240601001', '澶氫緵搴斿晢', '鏈叆搴?, 2, 180, 0, '涓嶈浆鍖?, '婕旂ず鏈叆搴撳崟鎹?, 'operator', 'operator'),
    (2, 'IN20240601002', '闀挎槬涓€姹介厤濂?, '閮ㄥ垎瀹屾垚', 2, 180, 70, '杞寘', '婕旂ず閮ㄥ垎瀹屾垚鍗曟嵁', 'operator', 'operator');

INSERT INTO inbound_order_detail (
    id, inbound_order_id, doc_no, line_no, supplier_code, supplier_name, material_code, material_name, package_model, packaging_capacity, planned_qty, actual_qty, package_count, warehouse_area, transfer_status, remark, created_by, updated_by
) VALUES
    (1, 1, 'IN20240601001', 1, 'SUP-001', '涓婃捣姹借溅闆堕儴浠?, 'MAT-ENG-001', '鍙戝姩鏈烘敮鏋?, 'BX-ENG-20', 20, 100, 0, 5, '榛樿搴撳尯', '涓嶈浆鍖?, '寰呭叆搴撴槑缁?, 'operator', 'operator'),
    (2, 1, 'IN20240601001', 2, 'SUP-002', '鑻忓窞绮惧瘑鍣ㄥ叿', 'MAT-TOOL-002', '瀹氫綅閿€', 'BOX-PIN-100', 100, 80, 0, 1, '榛樿搴撳尯', '涓嶈浆鍖?, '寰呭叆搴撴槑缁?, 'operator', 'operator'),
    (3, 2, 'IN20240601002', 1, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍', 'MAT-ELE-001', '鎺у埗鍣ㄦā鍧?, 'BOX-ELE-8', 8, 60, 30, 8, '榛樿搴撳尯', '涓嶈浆鍖?, '宸查儴鍒嗗叆搴?, 'operator', 'operator'),
    (4, 2, 'IN20240601002', 2, 'SUP-003', '瀹佹尝鐢靛瓙妯＄粍', 'MAT-ELE-002', '绾挎潫缁勪欢', 'BOX-HAR-15', 15, 120, 40, 8, '榛樿搴撳尯', '涓嶈浆鍖?, '宸查儴鍒嗗叆搴?, 'operator', 'operator');

INSERT INTO inventory_stock (
    id, material_code, material_name, supplier, on_hand_qty, last_inbound_doc_no, last_inbound_at, transfer_status, warehouse_area, created_by, updated_by
) VALUES
    (1, 'MAT-ELE-001', '鎺у埗鍣ㄦā鍧?, '瀹佹尝鐢靛瓙妯＄粍', 30, 'IN20240601002', '2026-06-07 10:00:00', '杞寘', '榛樿搴撳尯', 'system', 'system'),
    (2, 'MAT-ELE-002', '绾挎潫缁勪欢', '瀹佹尝鐢靛瓙妯＄粍', 40, 'IN20240601002', '2026-06-07 10:00:00', '杞寘', '榛樿搴撳尯', 'system', 'system');

-- ============================================================================
-- 6. 鍑哄簱鍗曡〃
-- ============================================================================
DROP TABLE IF EXISTS outbound_order_detail;
DROP TABLE IF EXISTS outbound_order;
CREATE TABLE outbound_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '涓婚敭ID',
    doc_no          VARCHAR(50)  NOT NULL UNIQUE       COMMENT '鍑哄簱鍗曞彿',
    supplier        VARCHAR(100) NOT NULL              COMMENT '渚涘簲鍟?,
    status          VARCHAR(20)  NOT NULL DEFAULT '寰呭嚭搴? COMMENT '鐘舵€侊細寰呭嚭搴?閮ㄥ垎瀹屾垚/宸插畬鎴?,
    item_count      INT          NOT NULL DEFAULT 0    COMMENT '鏄庣粏鏉℃暟',
    planned_total_qty INT       NOT NULL DEFAULT 0    COMMENT '璁″垝鍑哄簱鎬绘暟',
    actual_total_qty INT        NOT NULL DEFAULT 0    COMMENT '瀹為檯鍑哄簱鎬绘暟',
    remark          VARCHAR(255) DEFAULT NULL          COMMENT '澶囨敞',
    created_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '鍒涘缓浜?,
    updated_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '鏇存柊浜?,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    INDEX idx_doc_no (doc_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍑哄簱鍗曡〃';

-- 鍑哄簱鍗曟槑缁嗚〃
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
    warehouse_area      VARCHAR(100) DEFAULT '榛樿搴撳尯',
    remark              VARCHAR(255) DEFAULT NULL,
    created_by          VARCHAR(50)  DEFAULT 'system',
    updated_by          VARCHAR(50)  DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (outbound_order_id) REFERENCES outbound_order (id),
    UNIQUE (outbound_order_id, supplier_code, material_code)
);

-- 鍑哄簱鍘嗗彶琛?
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
    warehouse_area      VARCHAR(100) DEFAULT '榛樿搴撳尯',
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

-- 7. 鍑哄簱鍗曟紨绀烘暟鎹?
INSERT INTO outbound_order (doc_no, supplier, status, created_by) VALUES
('OUT20240601001', '骞垮窞鏈敯', '寰呭嚭搴?, 'operator'),
('OUT20240601002', '姝︽眽涓滈', '寰呭嚭搴?, 'operator')
ON DUPLICATE KEY UPDATE doc_no = VALUES(doc_no);




