package com.bharatshop.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final int MAX_PAYLOAD = 4096;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(req, res);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String method = req.getMethod();
            String uri = req.getRequestURI() + (req.getQueryString() != null ? ("?" + req.getQueryString()) : "");

            int status = res.getStatus();
            String contentType = res.getContentType();
            String correlationId = res.getHeader(CorrelationIdFilter.CORRELATION_HEADER);

            if (log.isInfoEnabled()) {
                log.info("[{}] {} {} -> {} ({} ms)", correlationId, method, uri, status, duration);
                
                String reqPayload = getPayload(req.getContentAsByteArray(), req.getCharacterEncoding());
                String resPayload = getPayload(res.getContentAsByteArray(), res.getCharacterEncoding());
                
                // Generate and log CURL
                String curl = generateCurl(req, reqPayload);
                log.info("[{}] CURL: {}", correlationId, curl);

                String authHeader = StringUtils.hasText(req.getHeader(HttpHeaders.AUTHORIZATION)) ? "present" : "absent";
                log.info("[{}] Headers: auth={} content-type={} accept={}", correlationId, authHeader, req.getContentType(), req.getHeader(HttpHeaders.ACCEPT));
                if (StringUtils.hasText(reqPayload)) {
                    log.info("[{}] Request payload: {}", correlationId, truncate(reqPayload));
                }
                if (StringUtils.hasText(resPayload)) {
                    log.info("[{}] Response payload: {}", correlationId, truncate(resPayload));
                }
            }

            // Make sure response body is written out
            res.copyBodyToResponse();
        }
    }

    private String generateCurl(ContentCachingRequestWrapper req, String body) {
        StringBuilder sb = new StringBuilder("curl");
        sb.append(" -X ").append(req.getMethod());
        sb.append(" '").append(req.getRequestURL());
        if (req.getQueryString() != null) {
            sb.append("?").append(req.getQueryString());
        }
        sb.append("'");

        java.util.Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = req.getHeader(name);
            sb.append(" -H '").append(name).append(": ").append(value).append("'");
        }

        if (body != null && !body.isBlank()) {
            // escape single quotes
            String escapedBody = body.replace("'", "'\\''");
            sb.append(" -d '").append(escapedBody).append("'");
        }
        return sb.toString();
    }

    private String getPayload(byte[] content, String enc) {
        if (content == null || content.length == 0) return null;
        try {
            String encoding = (enc != null) ? enc : StandardCharsets.UTF_8.name();
            return new String(content, encoding);
        } catch (Exception e) {
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        if (s.length() <= MAX_PAYLOAD) return s;
        return s.substring(0, MAX_PAYLOAD) + "... (truncated)";
    }
}