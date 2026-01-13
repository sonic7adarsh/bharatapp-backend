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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", 
                                "/health",
                                // Dev/public docs and health endpoints
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/api/auth/**",
                                "/api/storefront/auth/**",
                                "/api/riders/login",
                                "/store/**",
                                "/store/cart/**",
                                "/api/storefront/cart/**",
                                "/api/storefront/products/**",
                                "/api/storefront/categories",
                                "/api/stores/**",
                                "/api/products",
                                "/api/availability"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/platform/products", "/api/platform/products/**").permitAll()
                        .requestMatchers(
                                "/api/seller/**",
                                "/api/logistics/**"
                        ).hasAnyRole("SELLER","VENDOR","ADMIN")
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                "/api/zones/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                "/api/riders/**"
                        ).hasRole("RIDER")
                        .requestMatchers(
                                "/api/storefront/orders",
                                "/api/storefront/checkout",
                                "/api/storefront/payments/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}