package com.pilipili.config;


import com.pilipili.service.UserService;
import com.pilipili.utils.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 21:30
 */
@EnableWebSecurity
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SecurityConfig {

    private final UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests()
                .antMatchers("/user/user/**").hasRole(Role.ROLE_USER.getCode())
                .antMatchers("/user/admin/**").hasRole(Role.ROLE_ADMIN.getCode())
                .antMatchers("/manage/**").hasRole(Role.ROLE_MANAGE.getCode())
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginProcessingUrl("/login")
                .and()
                .csrf()
                //关闭 csrf 防御机制，这个 disable 方法本质上就是从 Spring Security 的过滤器链上移除掉 csrf 过滤器
                .disable();
        return  httpSecurity.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> web.ignoring().antMatchers("/doLogin");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
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
