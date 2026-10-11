package com.gamerift.shared.config;

import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.security.JwtAuthFilter;
import com.gamerift.shared.security.JwtUtil;
import com.gamerift.shared.security.RestAccessDeniedHandler;
import com.gamerift.shared.security.RestAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security rules (layer: configuration). PHASE 6: JWT + roles.
 *
 *  - stateless: no HTTP session, every request carries its own token,
 *  - CSRF is off because we do not use cookies for login,
 *  - CORS uses the bean from CorsConfig,
 *  - public: register, login, Swagger, health check,
 *  - /api/admin/** needs the ADMIN role, everything else needs a valid login,
 *  - JwtAuthFilter reads the token before the standard login filter,
 *  - @EnableMethodSecurity turns on @PreAuthorize on controller methods.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public SecurityConfig(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, userRepository);

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // JSON answers for 401 (not logged in) and 403 (wrong role)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(new RestAccessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // /me needs a login; every other /api/auth/** URL (register, login, OTP, reset) is public
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // numbers for the public landing page
                .requestMatchers("/api/stats/public").permitAll()
                // SSLCommerz calls these back itself (no JWT); every callback re-validates the payment
                .requestMatchers("/api/payment/sslcommerz/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * BCrypt turns a password into a one-way hash with a random salt.
     * We store only the hash; we can check a password later but never read it back.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
