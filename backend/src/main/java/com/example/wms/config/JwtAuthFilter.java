package com.example.wms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 请求过滤器
 *
 * 每次请求都会经过此过滤器：
 * 1. 从请求头 Authorization 中提取 Token
 * 2. 校验 Token 有效性
 * 3. 将用户信息注入 Spring Security 上下文
 *
 * 这样 Controller 层就可以通过 SecurityContextHolder 获取当前用户
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    /** 请求头名称 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头获取 Token
        String token = extractToken(request);

        // 2. 如果 Token 存在且有效，设置认证信息
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            log.debug("JWT 认证成功 - 用户: {}, 角色: {}", username, role);

            // 将角色转换为 Spring Security 的 GrantedAuthority
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

            // 创建认证对象（密码设为 null，因为 JWT 已经验证过身份）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority));

            // 注入到 SecurityContext，后续请求可直接使用
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
