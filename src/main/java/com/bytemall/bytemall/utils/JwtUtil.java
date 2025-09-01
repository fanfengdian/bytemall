package com.bytemall.bytemall.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
// 移除 @Component, @Resource, @PostConstruct
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// 没有 @Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    // 只有一个带参数的构造函数
    public JwtUtil(String secretKeyString, long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        this.expiration = expirationTime;
    }

    // ... (所有其他方法，如 generateToken, extractUsername 等，完全保持不变)
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