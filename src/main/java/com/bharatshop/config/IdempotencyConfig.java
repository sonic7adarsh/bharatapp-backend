package com.bharatshop.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotencyConfig {

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter() {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyFilter());
        registration.setOrder(3); // After correlation and http logging
        registration.addUrlPatterns("/*");
        return registration;
    }
}