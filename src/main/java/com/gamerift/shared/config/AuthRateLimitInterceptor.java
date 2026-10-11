package com.gamerift.shared.config;

import java.time.Duration;

import com.gamerift.shared.util.RateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every /api/auth/** and /api/payment/sslcommerz/** POST request.
 * Auth POSTs allow at most 30 per minute per IP; gateway callbacks allow 60, so valid
 * retries from SSLCommerz are not blocked. This slows down bots that guess passwords
 * or hammer the payment endpoints. (Per-account limits are in the services.)
 */
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public AuthRateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equals(request.getMethod())) {
            boolean paymentCallback = request.getRequestURI().startsWith("/api/payment/sslcommerz/");
            int max = paymentCallback ? 60 : 30;
            String prefix = paymentCallback ? "pay:" : "ip:";
            rateLimiter.check(prefix + request.getRemoteAddr(), max, Duration.ofMinutes(1), "Too many requests. Please slow down.");
        }
        return true;
    }
}
