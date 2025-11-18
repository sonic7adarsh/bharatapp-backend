package com.bharatshop.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String REQUEST_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, jakarta.servlet.ServletException {
        String headerId = request.getHeader(CORRELATION_HEADER);
        if (headerId == null || headerId.isBlank()) {
            headerId = request.getHeader(REQUEST_HEADER);
        }
        String traceId = (headerId == null || headerId.isBlank()) ? UUID.randomUUID().toString() : headerId;
        MDC.put(MDC_KEY, traceId);
        response.setHeader(CORRELATION_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}