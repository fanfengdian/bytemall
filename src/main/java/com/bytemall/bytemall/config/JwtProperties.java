package com.bytemall.bytemall.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bytemall.jwt") // 绑定配置文件中以 bytemall.jwt 开头的属性
public class JwtProperties {

    // 默认值，如果配置文件里没有，就用这个
    private String secretKey = "ByteMallSecretKeyForJwtTokenGenerationAndValidationSpecial";
    private Long expiration = 86400000L; // 默认24小时 (单位：毫秒)
}
