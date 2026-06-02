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
-- 3. 入库单表
-- ============================================================================
DROP TABLE IF EXISTS inbound_order;
CREATE TABLE inbound_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY  COMMENT '主键ID',
    doc_no          VARCHAR(50)  NOT NULL UNIQUE       COMMENT '入库单号',
    supplier        VARCHAR(100) NOT NULL              COMMENT '供应商',
    status          VARCHAR(20)  NOT NULL DEFAULT '未入库' COMMENT '状态：未入库/部分完成/已完成',
    created_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '创建人',
    updated_by      VARCHAR(50)  DEFAULT 'system'     COMMENT '更新人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_doc_no (doc_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- 4. 入库单演示数据
INSERT INTO inbound_order (doc_no, supplier, status, created_by) VALUES
('IN20240601001', '上海汽车零部件', '未入库', 'operator'),
('IN20240601002', '长春一汽配套', '部分完成', 'operator')
ON DUPLICATE KEY UPDATE doc_no = VALUES(doc_no);

-- ============================================================================
-- 5. 出库单表
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

-- 6. 出库单演示数据
INSERT INTO outbound_order (doc_no, supplier, status, created_by) VALUES
('OUT20240601001', '广州本田', '待出库', 'operator'),
('OUT20240601002', '武汉东风', '待出库', 'operator')
ON DUPLICATE KEY UPDATE doc_no = VALUES(doc_no);
