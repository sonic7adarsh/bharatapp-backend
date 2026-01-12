package com.bharatshop.config;

import com.bharatshop.security.TokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
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
                                "/api/storefront/auth/**",
                                "/store/cart/**",
                                "/api/storefront/products/**",
                                "/api/storefront/categories",
                                "/api/stores/**",
                                "/api/products",
                                "/api/availability",
                                "/api/platform/products" // GET allowed, POST will be protected in controller
                        ).permitAll()
                        .requestMatchers(
                                "/api/storefront/orders",
                                "/api/storefront/checkout",
                                "/api/storefront/payments/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}