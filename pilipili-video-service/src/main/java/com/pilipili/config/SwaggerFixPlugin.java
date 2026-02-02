package com.pilipili.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

/**
 * Swagger修复插件
 * 修复Swagger处理空字符串默认值时的NumberFormatException
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Configuration
public class SwaggerFixPlugin {

    @Bean
    public ParameterBuilderPlugin swaggerParameterBuilderPlugin() {
        return new ParameterBuilderPlugin() {
            @Override
            public void apply(ParameterContext context) {
                ParameterBuilder builder = context.parameterBuilder();
                try {
                    String defaultValue = builder.build().getDefaultValue();
                    // 如果默认值是空字符串，设置为null
                    if ("".equals(defaultValue)) {
                        builder.defaultValue(null);
                    }
                } catch (Exception e) {
                    // 忽略异常，设置为null
                    builder.defaultValue(null);
                }
            }

            @Override
            public boolean supports(DocumentationType delimiter) {
                return SwaggerPluginSupport.pluginDoesApply(delimiter);
            }
        };
    }
}
