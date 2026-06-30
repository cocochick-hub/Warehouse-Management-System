package com.example.wms.service.impl;

import com.example.wms.config.JwtUtil;
import com.example.wms.dto.LoginRequest;
import com.example.wms.dto.LoginResponse;
import com.example.wms.dto.UserInfoDTO;
import com.example.wms.dto.admin.CreateUserRequest;
import com.example.wms.dto.admin.ManagedUserDTO;
import com.example.wms.entity.SysUser;
import com.example.wms.repository.UserRepository;
import com.example.wms.service.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final Set<String> ALLOWED_ROLES = Set.of("admin", "manager", "operator");

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
                user.getAvatar(),
                user.getPhone()
        );

        return new LoginResponse(token, "Bearer", userInfo);
    }

    @Override
    public LoginResponse refreshToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("无效的认证头");
        }
        String oldToken = authHeader.substring(7).trim();
        // 从过期 token 中提取用户名（过期 7 天内可刷新）
        String username = jwtUtil.getUsernameFromExpiredToken(oldToken);
        // 查找用户确保仍有效
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BadCredentialsException("该账号已被禁用");
        }
        // 签发新 token
        String newToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        UserInfoDTO userInfo = new UserInfoDTO(
                user.getId(), user.getUsername(), user.getRealName(),
                user.getRole(), user.getAvatar(), user.getPhone());
        return new LoginResponse(newToken, "Bearer", userInfo);
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
                user.getAvatar(),
                user.getPhone()
        );
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserInfoDTO updateUserInfo(String username, String phone) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        user.setPhone(phone);
        userRepository.save(user);

        return new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getAvatar(),
                user.getPhone()
        );
    }

    @Override
    public List<ManagedUserDTO> listManagedUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .map(this::toManagedUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ManagedUserDTO createUser(CreateUserRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(trimToNull(request.getRealName()));
        user.setRole(normalizeRole(request.getRole()));
        user.setStatus(1);
        user.setPhone(trimToNull(request.getPhone()));
        user.setCreatedBy("admin");
        user.setUpdatedBy("admin");
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());

        return toManagedUserDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public ManagedUserDTO updateUserRole(Long id, String role) {
        SysUser user = getUserById(id);
        user.setRole(normalizeRole(role));
        user.setUpdatedBy("admin");
        return toManagedUserDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public ManagedUserDTO updateUserStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("用户状态只能是启用或禁用");
        }

        SysUser user = getUserById(id);
        user.setStatus(status);
        user.setUpdatedBy("admin");
        return toManagedUserDTO(userRepository.save(user));
    }

    private SysUser getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim();
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("角色只能是 admin、manager 或 operator");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ManagedUserDTO toManagedUserDTO(SysUser user) {
        return new ManagedUserDTO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getStatus(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
