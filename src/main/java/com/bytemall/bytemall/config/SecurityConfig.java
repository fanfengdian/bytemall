package com.bytemall.bytemall.config;

import com.bytemall.bytemall.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用BCrypt强哈希函数加密密码
        return new BCryptPasswordEncoder();
    }

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter; // 注入我们自定义的过滤器

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 配置请求授权
                .authorizeHttpRequests(authorize -> authorize
                        // 对于注册接口，允许所有用户访问（包括未登录的）
                        .requestMatchers("/members/register", "/members/login").permitAll()
                        // 2. 新增：允许任何人通过GET方法访问/products下的所有路径
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        // 对于其他所有请求，都必须经过身份验证
                        //.anyRequest().authenticated()
                        .anyRequest().hasRole("USER")
                )
                // 禁用CSRF保护，因为我们是无状态的API，不使用Cookie/Session
                .csrf(csrf->csrf.disable())
                // **新增配置：** 设置Session管理策略为无状态（STATELESS）
                // 这告诉Spring Security不要创建或使用HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // **新增配置：** 将我们的JWT过滤器添加到UsernamePasswordAuthenticationFilter之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);;

        return http.build();
    }
}
