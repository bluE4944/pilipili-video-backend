package com.pilipili.filter;

import com.pilipili.entity.User;
import com.pilipili.repository.UserRepository;
import com.pilipili.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            
            if (token != null && jwtUtil.validateToken(token)) {
                try {
                    String username = jwtUtil.getUsernameFromToken(token);
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    
                    log.debug("JWT Token解析: username={}, userId={}, role={}", username, userId, role);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 从数据库加载用户信息
                        User user = userRepository.getById(userId);
                        if (user != null && user.getUsername().equals(username)) {
                            // 使用用户数据库中的角色（确保一致性）
                            String userRole = user.getRole();
                            // 确保角色有ROLE_前缀（Spring Security要求）
                            String authority = userRole;
                            if (userRole != null && !userRole.startsWith("ROLE_")) {
                                authority = "ROLE_" + userRole;
                            }
                            
                            log.debug("设置认证信息: userId={}, username={}, authority={}", userId, username, authority);
                            
                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(authority))
                                );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            // 设置到Security上下文
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("JWT认证成功: userId={}, username={}, authority={}", userId, username, authority);
                        } else {
                            log.warn("用户不存在或用户名不匹配: userId={}, username={}", userId, username);
                        }
                    }
                } catch (Exception e) {
                    log.error("JWT认证失败", e);
                }
            } else {
                log.debug("Token无效或已过期");
            }
        } else {
            log.debug("请求头中未找到Authorization或格式不正确");
        }
        
        filterChain.doFilter(request, response);
    }
}
