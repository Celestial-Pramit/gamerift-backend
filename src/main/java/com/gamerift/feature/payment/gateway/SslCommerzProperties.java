package com.gamerift.feature.payment.gateway;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for the SSLCommerz sandbox gateway (layer: configuration).
 *
 * Spring fills these fields from application.yml under "app.payment.sslcommerz".
 * The store id and password themselves come from backend/.env, never from the repo.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment.sslcommerz")
public class SslCommerzProperties {

    /** Where the gateway lives: the sandbox URL in development, the live URL in production. */
    private String baseUrl;

    /** The merchant store id given by SSLCommerz. */
    private String storeId;

    /** The merchant store password given by SSLCommerz. Never logged. */
    private String storePasswd;

    /** True while we test with the sandbox. Kept so the code can tell the two apart if needed. */
    private boolean sandbox;
}
