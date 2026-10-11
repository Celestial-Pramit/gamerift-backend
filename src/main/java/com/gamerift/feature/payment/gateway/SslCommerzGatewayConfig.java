package com.gamerift.feature.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway startup diagnostics.
 * Logs a warning if BACKEND_URL points to localhost, because SSLCommerz callbacks
 * and IPN cannot reach a local address from the internet.
 */
@Configuration
public class SslCommerzGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(SslCommerzGatewayConfig.class);

    @Bean
    ApplicationRunner sslCommerzStartupCheck(@Value("${app.public-url}") String publicUrl) {
        return args -> {
            if (publicUrl != null && (publicUrl.startsWith("http://localhost") || publicUrl.startsWith("http://127.0.0.1"))) {
                log.warn("BACKEND_URL points to localhost: SSLCommerz callbacks and IPN cannot reach this server. Start a tunnel and set BACKEND_URL.");
            }
        };
    }
}