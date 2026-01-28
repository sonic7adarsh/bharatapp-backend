package com.bharatshop.config;

import com.bharatshop.security.TokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter) throws Exception {
        // Spring Security 6 style: explicit matchers, global OPTIONS, CORS enabled
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .anonymous(Customizer.withDefaults())
                // Ensure our token filter runs only on secured endpoints (filter itself skips public paths)
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Allow browser preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // SECURED STOREFRONT ENDPOINTS (Must come BEFORE public wildcard)
                        // Payments and Checkout MUST be authenticated
                        .requestMatchers("/api/storefront/payments/**").authenticated()
                        .requestMatchers("/api/storefront/checkout/**").authenticated()

                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/storefront/**", // Products, Categories etc remain public
                                "/api/location/**",
                                "/api/categories/**",
                                "/api/products/**",
                                "/api/search/**",
                                "/error",
                                "/actuator/**"
                        ).permitAll()
                        // Rider onboarding: allow any authenticated user, even without RIDER role
                        .requestMatchers(HttpMethod.POST, "/api/rider/onboard").authenticated()
                        // API docs / Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Actuator
                        // Role-based endpoints
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/rider/**").hasRole("RIDER")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(401);
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                );
        return http.build();
    }

    // CORS configuration for React frontend (adjust origins as needed for environments)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow all origins in production and any localhost ports in dev via patterns
        config.setAllowedOriginPatterns(List.of("*"));
        // Allow all headers & methods; preflight should always succeed
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        // Optionally expose common headers used by frontend
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Correlation-Id", "X-Idempotency-Key"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}