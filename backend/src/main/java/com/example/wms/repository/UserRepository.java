package com.example.wms.repository;

import com.example.wms.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 * JpaRepository 提供了基础的 CRUD 方法
 */
@Repository
public interface UserRepository extends JpaRepository<SysUser, Long> {

    /** 根据用户名查询用户（用于登录校验） */
    Optional<SysUser> findByUsername(String username);

    /** 判断用户名是否已存在 */
    boolean existsByUsername(String username);
}
