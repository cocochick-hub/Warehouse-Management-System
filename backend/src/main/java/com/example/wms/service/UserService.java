package com.example.wms.service;

import com.example.wms.dto.LoginRequest;
import com.example.wms.dto.LoginResponse;
import com.example.wms.dto.UserInfoDTO;

/**
 * 用户认证服务接口
 */
public interface UserService {

    /**
     * 用户登录
     * @param request 登录请求（用户名 + 密码）
     * @return 登录响应（Token + 用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息 DTO
     */
    UserInfoDTO getUserInfo(String username);
}
