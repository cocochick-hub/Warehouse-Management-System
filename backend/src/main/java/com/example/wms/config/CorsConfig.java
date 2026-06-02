package com.example.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（CORS）
 *
 * 前后端分离架构下，前端（如 http://localhost:5173）
 * 需要跨域访问后端 API（http://localhost:8080）
 * 此配置允许跨域请求并携带认证信息
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许前端域名（开发环境）
        config.addAllowedOriginPattern("*");
        // 允许携带凭证（Cookie/Authorization 头）
        config.setAllowCredentials(true);
        // 允许的 HTTP 方法
        config.addAllowedMethod("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 暴露的响应头（前端可获取）
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
