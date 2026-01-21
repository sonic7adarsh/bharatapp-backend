package com.bharatshop.config;

import com.bharatshop.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantInterceptor implements HandlerInterceptor {
    private static final String TENANT_HEADER = "X-Tenant-Domain";
    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenant = request.getHeader(TENANT_HEADER);
        if (tenant != null && !tenant.isBlank()) {
            TenantContext.setTenant(tenant);
            log.info("tenant-context: tenant={} method={} path={}", tenant, request.getMethod(), request.getRequestURI());
            return true;
        } else {
            // Fail fast if tenant header missing for API routes
            log.warn("tenant-context: missing header method={} path={}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try { response.getWriter().write("Missing X-Tenant-Domain header"); } catch (Exception ignored) {}
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}