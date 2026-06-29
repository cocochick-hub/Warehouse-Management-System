package com.example.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WMS 仓库管理系统 - 后端启动入口
 *
 * 启动后访问：
 * - 登录接口：POST http://localhost:8080/api/auth/login
 * - 用户信息：GET  http://localhost:8080/api/auth/userInfo
 */
@SpringBootApplication
@EnableScheduling
public class WmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsApplication.class, args);
    }
}
