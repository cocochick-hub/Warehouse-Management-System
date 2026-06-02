package com.example.wms.config;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具类
 *
 * 功能：
 * - 生成 Token（包含用户名和角色）
 * - 从 Token 中解析用户名
 * - 校验 Token 是否过期/有效
 *
 * Token 结构：Header.Payload.Signature
 * Payload 包含：sub=用户名, role=角色, iat=签发时间, exp=过期时间
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private String base64Secret;

    @PostConstruct
    public void init() {
        // 将密钥进行 Base64 编码
        base64Secret = Base64.getEncoder().encodeToString(secret.getBytes());
    }

    /**
     * 生成 JWT Token
     * @param username 用户名
     * @param role     用户角色
     * @return JWT 字符串
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)                    // 主体：用户名
                .claim("role", role)                     // 自定义声明：角色
                .setIssuedAt(now)                        // 签发时间
                .setExpiration(expiryDate)               // 过期时间
                .signWith(SignatureAlgorithm.HS256, base64Secret)  // 签名算法 + 密钥
                .compact();
    }

    /**
     * 从 Token 中解析用户名
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 从 Token 中解析角色
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 签名校验失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token 为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 解析 JWT Claims（私有方法）
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(base64Secret)
                .parseClaimsJws(token)
                .getBody();
    }
}
