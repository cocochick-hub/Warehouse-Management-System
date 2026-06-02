package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.LoginRequest;
import com.example.wms.dto.LoginResponse;
import com.example.wms.dto.UserInfoDTO;
import com.example.wms.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证控制器 - 处理登录、获取用户信息等认证相关请求
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录
     * @param request 用户名 + 密码
     * @return ApiResult<LoginResponse> 包含 Token 和用户信息
     */
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResult.success("登录成功", response);
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/userInfo")
    public ApiResult<UserInfoDTO> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserInfoDTO userInfo = userService.getUserInfo(username);
        return ApiResult.success(userInfo);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.success("退出登录成功", null);
    }
}