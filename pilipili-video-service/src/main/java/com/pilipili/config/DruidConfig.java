package com.pilipili.config;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Druid数据库连接池配置
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Configuration
public class DruidConfig {

    /**
     * 注册StatViewServlet（Druid监控页面）
     * 访问地址：http://localhost:8316/druid/index.html
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> registrationBean = 
            new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
        
        // 设置控制台管理用户
        registrationBean.addInitParameter("loginUsername", "admin");
        registrationBean.addInitParameter("loginPassword", "admin123");
        // 禁用HTML页面上的"Reset All"功能
        registrationBean.addInitParameter("resetEnable", "false");
        // IP白名单(没有配置或者为空，则允许所有访问)
        registrationBean.addInitParameter("allow", "127.0.0.1,192.168.163.1");
        // IP黑名单 (存在共同时，deny优先于allow)
        registrationBean.addInitParameter("deny", "192.168.1.73");
        
        log.info("Druid监控页面已配置，访问地址：http://localhost:8316/druid/index.html");
        return registrationBean;
    }

    /**
     * 注册WebStatFilter（用于统计Web应用监控信息）
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> filterRegistrationBean = 
            new FilterRegistrationBean<>(new WebStatFilter());
        
        // 添加过滤规则
        filterRegistrationBean.addUrlPatterns("/*");
        // 添加不需要忽略的格式信息
        filterRegistrationBean.addInitParameter("exclusions", 
            "*.js,*.gif,*.jpg,*.bmp,*.png,*.css,*.ico,/druid/*");
        
        return filterRegistrationBean;
    }
}
