package com.bharatshop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Value("${app.tenant.header:X-Tenant-Domain}")
    private String tenantHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/storefront")) {
            String tenant = request.getHeader(tenantHeader);
            if (tenant == null || tenant.isBlank()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                String body = "{"+
                        "\"code\":\"VALIDATION_ERROR\","+
                        "\"message\":\"Missing X-Tenant-Domain header\","+
                        "\"details\":{\"header\":\"" + tenantHeader + "\"}}";
                response.getWriter().write(body);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}