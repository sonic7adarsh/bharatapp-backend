package com.bharatshop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String HEADER = "Idempotency-Key";
    private static final String ALT_HEADER = "X-Idempotency-Key";
    private static final long TTL_MS = 10 * 60 * 1000; // 10 minutes

    private static class CacheEntry {
        final int status;
        final byte[] body;
        final String contentType;
        final long storedAt;

        CacheEntry(int status, byte[] body, String contentType) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
            this.storedAt = System.currentTimeMillis();
        }

        boolean expired() { return (System.currentTimeMillis() - storedAt) > TTL_MS; }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        // Accept both standard and X- prefixed header for compatibility with docs/clients
        String key = request.getHeader(HEADER);
        if (!StringUtils.hasText(key)) {
            key = request.getHeader(ALT_HEADER);
        }
        if (!isMutating(method) || !StringUtils.hasText(key)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        String cacheKey = buildCacheKey(req, key);
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.expired()) {
            log.info("Idempotency hit: key={} method={} uri={}", key, method, request.getRequestURI());
            res.setStatus(entry.status);
            if (entry.contentType != null) {
                res.setContentType(entry.contentType);
            }
            res.getOutputStream().write(entry.body);
            res.copyBodyToResponse();
            return;
        }

        // Proceed and cache on success
        filterChain.doFilter(req, res);
        byte[] responseBody = res.getContentAsByteArray();
        String contentType = res.getContentType();
        int status = res.getStatus();
        if (shouldCache(status)) {
            cache.put(cacheKey, new CacheEntry(status, responseBody, contentType));
            log.info("Idempotency store: key={} method={} uri={} status={} at={}", key, method, request.getRequestURI(), status, Instant.now());
        }
        res.copyBodyToResponse();
    }

    private boolean isMutating(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method);
    }

    private boolean shouldCache(int status) { return status >= 200 && status < 300; }

    private String buildCacheKey(ContentCachingRequestWrapper req, String key) {
        String uri = req.getRequestURI();
        String method = req.getMethod();
        String tenant = req.getHeader("X-Tenant-Domain");
        String auth = req.getHeader("Authorization");
        String body = new String(req.getContentAsByteArray(), StandardCharsets.UTF_8);
        return String.join("|", key, method, uri, tenant != null ? tenant : "-", auth != null ? auth : "-", body);
    }
}