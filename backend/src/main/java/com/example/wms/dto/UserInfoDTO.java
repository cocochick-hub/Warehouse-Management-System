package com.example.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息（脱敏后返回前端，不包含密码）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色 */
    private String role;

    /** 头像 */
    private String avatar;

    /** 联系电话 */
    private String phone;
}
