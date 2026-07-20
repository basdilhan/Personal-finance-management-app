package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserSyncInterceptor userSyncInterceptor;

    public WebConfig(UserSyncInterceptor userSyncInterceptor) {
        this.userSyncInterceptor = userSyncInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userSyncInterceptor).addPathPatterns("/api/**");
    }
}
