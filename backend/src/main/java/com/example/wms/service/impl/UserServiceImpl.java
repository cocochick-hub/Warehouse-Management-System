package com.example.wms.service.impl;

import com.example.wms.config.JwtUtil;
import com.example.wms.dto.LoginRequest;
import com.example.wms.dto.LoginResponse;
import com.example.wms.dto.UserInfoDTO;
import com.example.wms.entity.SysUser;
import com.example.wms.repository.UserRepository;
import com.example.wms.service.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户认证服务实现
 *
 * 登录流程：
 * 1. 根据用户名查询用户
 * 2. 使用 BCrypt 比对密码
 * 3. 校验用户状态（是否禁用）
 * 4. 生成 JWT Token 并返回
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查找用户
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));

        // 2. 校验密码（BCrypt）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 3. 校验状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BadCredentialsException("该账号已被禁用，请联系管理员");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        // 5. 组装响应
        UserInfoDTO userInfo = new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getAvatar()
        );

        return new LoginResponse(token, "Bearer", userInfo);
    }

    @Override
    public UserInfoDTO getUserInfo(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        return new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getAvatar()
        );
    }
}
