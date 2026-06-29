package com.example.wms.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedUserDTO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private Integer status;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
