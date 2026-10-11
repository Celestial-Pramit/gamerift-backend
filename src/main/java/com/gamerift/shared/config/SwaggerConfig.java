package com.gamerift.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title and description shown at the top of the Swagger UI page
 * (http://localhost:9090/swagger-ui.html) and the "Authorize" button for the JWT token.
 */
@Configuration
public class SwaggerConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI gameriftOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gamerift API")
                        .version("0.2.0")
                        .description("Backend for the Gamerift game streaming platform (Advanced Java course project)."))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
