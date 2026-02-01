package com.bharatshop.config;

import com.bharatshop.config.logging.CorrelationIdFilter;
import com.bharatshop.config.logging.HttpLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilter() {
        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpLoggingFilter());
        registration.setOrder(2);
        registration.addUrlPatterns("/*");
        return registration;
    }
}