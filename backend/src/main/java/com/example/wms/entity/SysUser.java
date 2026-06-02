package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

/**
 * 系统用户实体 - 对应 sys_user 表
 * 用于登录认证与权限区分
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（登录账号） */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 密码（BCrypt 加密存储） */
    @Column(nullable = false, length = 255)
    private String password;

    /** 真实姓名 */
    @Column(name = "real_name", length = 50)
    private String realName;

    /**
     * 角色：
     * admin    - 系统管理员（拥有全部权限）
     * operator - 操作员（日常出入库操作）
     * manager  - 仓库经理（审核与报表查看）
     */
    @Column(nullable = false, length = 20)
    private String role;

    /** 状态：1-启用 / 0-禁用 */
    @Column(nullable = false)
    private Integer status;

    /** 头像 URL */
    @Column(length = 255)
    private String avatar;
}
