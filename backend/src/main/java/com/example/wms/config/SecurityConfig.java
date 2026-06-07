package com.example.wms.config;

import com.example.wms.dto.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 安全配置
 *
 * 核心策略：
 * 1. 登录接口 /api/auth/login 无需认证，允许匿名访问
 * 2. 其余所有 API 均需携带有效 JWT Token
 * 3. 无状态 Session（不创建也不使用 HttpSession）
 * 4. 注册 JWT 过滤器，在 UsernamePasswordAuthenticationFilter 之前执行
 * 5. 启用 CORS，使用自定义 CorsFilter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 BCrypt 强哈希算法加密密码
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 1. 启用 CORS
            .cors().and()

            // 2. 禁用 CSRF（前后端分离使用 Token，不需要 CSRF）
            .csrf().disable()

            // 3. 无状态 Session
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            // 4. 配置请求权限
            .authorizeRequests()
            // 放行 OPTIONS 预检请求
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // 登录接口放行（无需认证）
            .antMatchers("/api/auth/login").permitAll()
            // 其他所有请求需要认证
            .anyRequest().authenticated()
            .and()

            // 4.1 统一未认证/无权限返回格式
            .exceptionHandling()
            .authenticationEntryPoint((request, response, ex) -> writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ApiResult.unauthorized("未登录或登录已过期")))
            .accessDeniedHandler((request, response, ex) -> writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    ApiResult.forbidden("无权限访问该资源")))
            .and()

            // 5. 注册 JWT 过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // 6. 禁用默认表单登录和 HTTP Basic
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable();
    }

    private void writeJson(HttpServletResponse response, int status, ApiResult<Void> body) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
