package com.gamerift.shared.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS = which website is allowed to call our API from a browser.
 * The React app runs on another origin (http://localhost:5173), so without this the browser
 * would block every request. We allow ONLY the frontend address from the settings.
 */
@Configuration
public class CorsConfig {

    // Read from application.yml (app.frontend-url), which reads FRONTEND_URL from .env
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // SSLCommerz calls our callback / IPN URLs cross-site. Those requests must bypass CORS:
        // returning null means "no CORS handling for this path", so a normal (non-preflight)
        // browser form-post is passed straight through instead of being rejected with 403.
        // It is safe because every callback is re-validated with SSLCommerz on our server.
        return request -> {
            String path = request.getRequestURI();
            if (path != null && path.startsWith("/api/payment/sslcommerz/")) {
                return null;
            }
            return source.getCorsConfiguration(request);
        };
    }
}
