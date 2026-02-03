package com.pilipili.config;


import com.pilipili.filter.JwtAuthenticationFilter;
import com.pilipili.service.UserService;
import com.pilipili.utils.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 21:30
 */
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(@Lazy UserService userService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userService = userService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors()
                .and()
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 公开接口：用户注册、登录、Swagger文档
                .antMatchers("/api/user/register", "/api/auth/**", "/doLogin", "/login", "/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs", "/webjars/**", "/swagger-ui.html").permitAll()
                // 公开接口：视频查询、搜索、播放（无需登录）
                .antMatchers("/api/video/page", "/api/video/{videoId}", "/api/video/search/**", "/api/video/play/url/**", "/api/video/danmaku/{videoId}", "/api/auth/login").permitAll()
                // 公开接口：视频统计（部分）
                .antMatchers("/api/video/statistics/video/**", "/api/video/statistics/hot/**").permitAll()
                // WebSocket端点
                .antMatchers("/ws/**").permitAll()
                // Druid监控页面（仅管理员可访问）
                .antMatchers("/druid/**").hasRole(Role.ROLE_ADMIN.getCode())
                // 用户相关接口需要USER角色
                .antMatchers("/api/user/**").hasAnyRole(Role.ROLE_USER.getCode(), Role.ROLE_MANAGE.getCode(), Role.ROLE_ADMIN.getCode())
                // 视频上传、修改、删除需要USER角色
                .antMatchers("/api/video/upload/**", "/api/video/{videoId}", "/api/video/{videoId}/status").hasAnyRole(Role.ROLE_USER.getCode(), Role.ROLE_MANAGE.getCode(), Role.ROLE_ADMIN.getCode())
                // 视频互动接口需要USER角色
                .antMatchers("/api/video/interaction/**").hasAnyRole(Role.ROLE_USER.getCode(), Role.ROLE_MANAGE.getCode(), Role.ROLE_ADMIN.getCode())
                // 播放进度记录需要USER角色
                .antMatchers("/api/video/play/progress/**").hasAnyRole(Role.ROLE_USER.getCode(), Role.ROLE_MANAGE.getCode(), Role.ROLE_ADMIN.getCode())
                // 弹幕发送需要USER角色
                .antMatchers("/api/video/danmaku").hasAnyRole(Role.ROLE_USER.getCode(), Role.ROLE_MANAGE.getCode(), Role.ROLE_ADMIN.getCode())
                // 系统配置需要ADMIN角色
                .antMatchers("/api/system/**").hasRole(Role.ROLE_ADMIN.getCode())
                // 本地文件夹配置需要ADMIN角色
                .antMatchers("/api/local-folder/**").hasRole(Role.ROLE_ADMIN.getCode())
                // 视频合集管理需要USER角色
                .antMatchers("/api/video/collection/**").hasAnyRole(Role.ROLE_USER.getCode(), Role.ROLE_MANAGE.getCode(), Role.ROLE_ADMIN.getCode())
                // 旧接口兼容
                .antMatchers("/user/user/**").hasRole(Role.ROLE_USER.getCode())
                .antMatchers("/user/admin/**").hasRole(Role.ROLE_ADMIN.getCode())
                .antMatchers("/manage/**").hasRole(Role.ROLE_MANAGE.getCode())
                .anyRequest().authenticated()
                .and()
                // 禁用表单登录，使用JWT Token认证
                .formLogin().disable()
                // 禁用HTTP Basic认证
                .httpBasic().disable()
                // 配置Session管理为无状态（使用JWT，不需要Session）
                .sessionManagement()
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                //关闭 csrf 防御机制，这个 disable 方法本质上就是从 Spring Security 的过滤器链上移除掉 csrf 过滤器
                .disable();
        return  httpSecurity.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> web.ignoring().antMatchers("/doLogin", "/error");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 角色继承 user 能访问的资源 admin都能访问
     * @return
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy(Role.ROLE_ADMIN.getCode() + " > " + Role.ROLE_MANAGE.getCode() + " > " + Role.ROLE_USER.getCode());
        return hierarchy;
    }

}
