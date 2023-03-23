package com.pilipili.config;


import com.pilipili.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
                .anyRequest().authenticated()
                .and()
                .formLogin()
                //配置登录页面，如果访问了一个需要认证之后才能访问的页面，那么就会自动跳转到这个页面上来
                .loginPage("/login")
                //配置处理登录请求的接口，本质上其实就是配置过滤器的拦截规则，将来的登录请求就会在过滤器中被处理
                .loginProcessingUrl("/doLogin")
                //配置登录表单中用户名的 key
                .usernameParameter("username")
                //配置登录表单中用户密码
                .passwordParameter("password")
                //配置登录成功后的跳转地址
                .defaultSuccessUrl("/")
                //失败地址
                .failureUrl("/login")
                .and()
                .csrf()
                //关闭 csrf 防御机制，这个 disable 方法本质上就是从 Spring Security 的过滤器链上移除掉 csrf 过滤器
                .disable();
        return  httpSecurity.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> web.ignoring().antMatchers("/user/login");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

}
