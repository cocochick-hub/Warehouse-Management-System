package com.example.wms.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateUserRoleRequest {

    @NotBlank(message = "角色不能为空")
    private String role;
}
