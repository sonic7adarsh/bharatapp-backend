package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    @Autowired
    private TenantConfigurationService tenantConfigurationService;
    
    // In-memory rate limiting storage (consider Redis for production)
    private final Map<String, RateLimitBucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, RateLimitBucket> tenantBuckets = new ConcurrentHashMap<>();
    
    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant resetTime;
        private final int limit;
        private final int windowMinutes;
        
        public RateLimitBucket(int limit, int windowMinutes) {
            this.limit = limit;
            this.windowMinutes = windowMinutes;
            this.resetTime = Instant.now().plus(windowMinutes, ChronoUnit.MINUTES);
        }
        
        public boolean tryConsume() {
            Instant now = Instant.now();
            if (now.isAfter(resetTime)) {
                reset(now);
            }
            
            int current = count.get();
            if (current >= limit) {
                return false;
            }
            
            return count.compareAndSet(current, current + 1);
        }
        
        public int getRemaining() {
            return Math.max(0, limit - count.get());
        }
        
        public int getLimit() {
            return limit;
        }
        
        public Instant getResetTime() {
            return resetTime;
        }
        
        private void reset(Instant now) {
            count.set(0);
            resetTime = now.plus(windowMinutes, ChronoUnit.MINUTES);
        }
    }
    
    public boolean isUserRateLimited(String userId, String tenantId) {
        try {
            int limit = tenantConfigurationService.getRateLimitPerUser(tenantId);
            String key = "user:" + userId + ":" + tenantId;
            
            RateLimitBucket bucket = userBuckets.computeIfAbsent(key, 
                k -> new RateLimitBucket(limit, 1)); // 1-minute window
            
            boolean allowed = bucket.tryConsume();
            
            if (!allowed) {
                logger.warn("User {} rate limited for tenant {}. Limit: {}, Remaining: {}", 
                    userId, tenantId, bucket.getLimit(), bucket.getRemaining());
            }
            
            return !allowed;
        } catch (Exception e) {
            logger.error("Error checking user rate limit for user {} and tenant {}", userId, tenantId, e);
            return false; // Allow on error
        }
    }
    
    public boolean isTenantRateLimited(String tenantId) {
        try {
            int limit = tenantConfigurationService.getRateLimitPerTenant(tenantId);
            String key = "tenant:" + tenantId;
            
            RateLimitBucket bucket = tenantBuckets.computeIfAbsent(key, 
                k -> new RateLimitBucket(limit, 1)); // 1-minute window
            
            boolean allowed = bucket.tryConsume();
            
            if (!allowed) {
                logger.warn("Tenant {} rate limited. Limit: {}, Remaining: {}", 
                    tenantId, bucket.getLimit(), bucket.getRemaining());
            }
            
            return !allowed;
        } catch (Exception e) {
            logger.error("Error checking tenant rate limit for tenant {}", tenantId, e);
            return false; // Allow on error
        }
    }
    
    public boolean checkRateLimit(String userId, String tenantId) {
        // Check both user and tenant rate limits
        boolean userLimited = isUserRateLimited(userId, tenantId);
        boolean tenantLimited = isTenantRateLimited(tenantId);
        
        return !userLimited && !tenantLimited;
    }
    
    public Map<String, Object> getRateLimitStatus(String userId, String tenantId) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // User rate limit status
            String userKey = "user:" + userId + ":" + tenantId;
            RateLimitBucket userBucket = userBuckets.get(userKey);
            
            if (userBucket != null) {
                status.put("userLimit", userBucket.getLimit());
                status.put("userRemaining", userBucket.getRemaining());
                status.put("userResetTime", userBucket.getResetTime().toString());
            } else {
                int userLimit = tenantConfigurationService.getRateLimitPerUser(tenantId);
                status.put("userLimit", userLimit);
                status.put("userRemaining", userLimit);
                status.put("userResetTime", Instant.now().plus(1, ChronoUnit.MINUTES).toString());
            }
            
            // Tenant rate limit status
            String tenantKey = "tenant:" + tenantId;
            RateLimitBucket tenantBucket = tenantBuckets.get(tenantKey);
            
            if (tenantBucket != null) {
                status.put("tenantLimit", tenantBucket.getLimit());
                status.put("tenantRemaining", tenantBucket.getRemaining());
                status.put("tenantResetTime", tenantBucket.getResetTime().toString());
            } else {
                int tenantLimit = tenantConfigurationService.getRateLimitPerTenant(tenantId);
                status.put("tenantLimit", tenantLimit);
                status.put("tenantRemaining", tenantLimit);
                status.put("tenantResetTime", Instant.now().plus(1, ChronoUnit.MINUTES).toString());
            }
            
            status.put("userId", userId);
            status.put("tenantId", tenantId);
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status for user {} and tenant {}", userId, tenantId, e);
            status.put("error", "Failed to get rate limit status");
        }
        
        return status;
    }
    
    public void clearUserRateLimit(String userId, String tenantId) {
        String key = "user:" + userId + ":" + tenantId;
        userBuckets.remove(key);
        logger.info("Cleared rate limit for user {} in tenant {}", userId, tenantId);
    }
    
    public void clearTenantRateLimit(String tenantId) {
        String key = "tenant:" + tenantId;
        tenantBuckets.remove(key);
        logger.info("Cleared rate limit for tenant {}", tenantId);
    }
    
    public void clearAllRateLimits() {
        userBuckets.clear();
        tenantBuckets.clear();
        logger.info("Cleared all rate limits");
    }
    
    public void cleanupExpiredBuckets() {
        Instant now = Instant.now();
        
        // Clean up expired user buckets
        userBuckets.entrySet().removeIf(entry -> {
            RateLimitBucket bucket = entry.getValue();
            return now.isAfter(bucket.getResetTime());
        });
        
        // Clean up expired tenant buckets
        tenantBuckets.entrySet().removeIf(entry -> {
            RateLimitBucket bucket = entry.getValue();
            return now.isAfter(bucket.getResetTime());
        });
        
        logger.debug("Cleaned up expired rate limit buckets");
    }
}