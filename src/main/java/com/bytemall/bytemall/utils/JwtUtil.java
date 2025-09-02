package com.bytemall.bytemall.utils;

import com.bytemall.bytemall.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component // <-- 加上注解
public class JwtUtil {

    @Resource // <-- 加上注解，注入配置
    private JwtProperties jwtProperties;

    private SecretKey secretKey;
    private long expiration;

    // 删除带参数的构造函数，改用PostConstruct
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
        this.expiration = jwtProperties.getExpiration();
    }

    // ... 其他所有方法保持不变 ...
    public String generateToken(String username, Long memberId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + this.expiration))
                .signWith(this.secretKey)
                .compact();
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(this.secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
    public Long extractMemberId(String token) {
        return extractAllClaims(token).get("memberId", Long.class);
    }
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}