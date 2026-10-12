package com.zzz.zerocode.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 启用 @SaCheckLogin / @SaCheckRole 注解鉴权。
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}
