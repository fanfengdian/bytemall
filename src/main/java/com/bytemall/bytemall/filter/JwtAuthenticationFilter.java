package com.bytemall.bytemall.filter;

import com.bytemall.bytemall.utils.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 从请求头中获取Authorization字段
        final String authHeader = request.getHeader("Authorization");

        // 2. 检查Authorization头是否存在，以及是否以"Bearer "开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // 如果不满足条件，直接放行，让后续的过滤器处理
            return;
        }

        // 3. 提取JWT令牌（去掉"Bearer "前缀）
        final String jwt = authHeader.substring(7);
        final String username;

        try {
            // 4. 从JWT中解析出用户名
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            // 如果解析失败（例如令牌过期、格式错误），直接放行
            // 后续的Spring Security过滤器会因为上下文中没有认证信息而拦截请求
            filterChain.doFilter(request, response);
            return;
        }


        // 5. 检查用户名是否存在，并且当前安全上下文中是否还没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 6. 验证令牌是否有效（这里我们只验证了格式和过期时间，可以增加和数据库用户的比对）
            // 在我们的简单实现中，只要能从有效令牌中解析出用户名，就认为它是有效的
            // 更复杂的场景下，你可能需要一个UserDetailsS.erv.ice来从数据库加载用户信息进行比对
            if (jwtUtil.validateToken(jwt, username)) {
                // 7. 如果令牌有效，创建一个Spring Security能理解的Authentication对象
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, // principal: 当事人，通常是用户名或用户对象
                        null,     // credentials: 凭证，对于JWT认证，我们不需要密码，所以是null
                        // authorities: 授予一个默认的用户角色权限
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 8. 将这个Authentication对象设置到安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. 无论验证成功与否，都放行请求，让它继续前进
        filterChain.doFilter(request, response);
    }
}

