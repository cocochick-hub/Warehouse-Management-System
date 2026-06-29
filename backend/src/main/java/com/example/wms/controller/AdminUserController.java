package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.admin.CreateUserRequest;
import com.example.wms.dto.admin.ManagedUserDTO;
import com.example.wms.dto.admin.UpdateUserRoleRequest;
import com.example.wms.dto.admin.UpdateUserStatusRequest;
import com.example.wms.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResult<List<ManagedUserDTO>> listUsers() {
        return ApiResult.success(userService.listManagedUsers());
    }

    @PostMapping
    public ApiResult<ManagedUserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResult.success("用户创建成功", userService.createUser(request));
    }

    @PutMapping("/{id}/role")
    public ApiResult<ManagedUserDTO> updateRole(@PathVariable Long id,
                                                @Valid @RequestBody UpdateUserRoleRequest request) {
        return ApiResult.success("角色更新成功", userService.updateUserRole(id, request.getRole()));
    }

    @PutMapping("/{id}/status")
    public ApiResult<ManagedUserDTO> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResult.success("状态更新成功", userService.updateUserStatus(id, request.getStatus()));
    }
}
