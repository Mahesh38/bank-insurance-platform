package com.bank.workforce.bff.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BffWebMvcConfig implements WebMvcConfigurer {

    private final BffSessionInterceptor sessionInterceptor;

    public BffWebMvcConfig(BffSessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor).addPathPatterns("/api/v1/**");
    }
}
