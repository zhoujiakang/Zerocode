package com.zzz.zerocode.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI zeroCodeOpenAPI() {
        return new OpenAPI().info(new Info().title("ZeroCode 接口文档")
                .description("zzz 的低代码应用生成平台").version("1.0.0"));
    }
}
