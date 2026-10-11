package com.gamerift.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the rate-limit interceptor for the auth URLs. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthRateLimitInterceptor authRateLimitInterceptor;

    public WebConfig(AuthRateLimitInterceptor authRateLimitInterceptor) {
        this.authRateLimitInterceptor = authRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns("/api/auth/**", "/api/payment/sslcommerz/**");
    }
}
