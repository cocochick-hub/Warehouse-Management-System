package com.example.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应
 * 返回 Token 和用户基本信息，前端将 Token 存储在本地用于后续请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT Token，前端需在后续请求头中携带 */
    private String token;

    /** Token 类型（固定为 Bearer） */
    private String tokenType;

    /** 用户信息 */
    private UserInfoDTO userInfo;
}
