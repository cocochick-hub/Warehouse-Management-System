package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.ChangePasswordRequest;
import com.example.wms.dto.LoginRequest;
import com.example.wms.dto.LoginResponse;
import com.example.wms.dto.UpdateUserInfoRequest;
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
     * 刷新 Token
     * 接受已过期的旧 token，验证签名后签发新 token
     * 过期不超过 7 天的 token 仍可刷新
     */
    @PostMapping("/refresh")
    public ApiResult<LoginResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        LoginResponse response = userService.refreshToken(authHeader);
        return ApiResult.success("Token 已刷新", response);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.success("退出登录成功", null);
    }

    /**
     * 修改密码
     * @param request 旧密码 + 新密码
     * @return 操作结果
     */
    @PutMapping("/changePassword")
    public ApiResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        userService.changePassword(username, request.getOldPassword(), request.getNewPassword());
        return ApiResult.success("密码修改成功", null);
    }

    /**
     * 更新用户信息
     * @param request 用户信息
     * @return 更新后的用户信息
     */
    @PutMapping("/userInfo")
    public ApiResult<UserInfoDTO> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserInfoDTO userInfo = userService.updateUserInfo(username, request.getPhone());
        return ApiResult.success("用户信息更新成功", userInfo);
    }
}