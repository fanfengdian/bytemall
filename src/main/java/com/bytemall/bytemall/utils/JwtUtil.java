package com.bytemall.bytemall.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    // 密钥：用于签名JWT。在真实项目中，这应该是一个非常复杂且从配置文件读取的字符串
    // 为了安全，密钥长度至少应该是256位（32个字符）
    private static final String SECRET_KEY_STRING = "ByteMallSecretKeyForJwtTokenGenerationAndValidation";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // 过期时间：设置为1天（单位：毫秒）
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    /**
     * 生成JWT令牌
     * @param username 用户名
     * @param memberId 用户ID
     * @return 生成的JWT字符串
     */
    public String generateToken(String username, Long memberId) {
        Map<String, Object> claims = new HashMap<>();
        // 你可以把任何非敏感信息放入claims中
        claims.put("memberId", memberId);

        return Jwts.builder()
                .claims(claims) // 设置自定义负载
                .subject(username) // 设置主题，通常是用户名
                .issuedAt(new Date(System.currentTimeMillis())) // 设置签发时间
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 设置过期时间
                .signWith(SECRET_KEY) // 使用密钥和指定的算法进行签名
                .compact(); // 生成JWT字符串
    }

    /**
     * 从JWT令牌中解析出Claims（负载）
     * @param token JWT字符串
     * @return 解析出的Claims对象
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY) // 使用相同的密钥进行验证
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从JWT令牌中获取用户名
     * @param token JWT字符串
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * 从JWT令牌中获取用户ID
     * @param token JWT字符串
     * @return 用户ID
     */
    public Long extractMemberId(String token) {
        return extractAllClaims(token).get("memberId", Long.class);
    }

    /**
     * 检查JWT令牌是否过期
     * @param token JWT字符串
     * @return 如果未过期则返回true
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * 验证JWT令牌是否有效
     * @param token JWT字符串
     * @param username 用于验证的用户名
     * @return 如果有效则返回true
     */
    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
