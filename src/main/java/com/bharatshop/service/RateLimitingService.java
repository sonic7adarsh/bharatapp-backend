package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    // @Autowired
    // private TenantConfigurationService tenantConfigurationService;
    
    // In-memory rate limiting storage (consider Redis for production)
    private final Map<String, RateLimitBucket> userBuckets = new ConcurrentHashMap<>();
    
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
    
    public boolean isUserRateLimited(String userId) {
        try {
            // int limit = tenantConfigurationService.getRateLimitPerUser();
            int limit = 100; // Default limit
            String key = "user:" + userId;
            
            RateLimitBucket bucket = userBuckets.computeIfAbsent(key, 
                k -> new RateLimitBucket(limit, 1)); // 1-minute window
            
            boolean allowed = bucket.tryConsume();
            
            if (!allowed) {
                logger.warn("User {} rate limited. Limit: {}, Remaining: {}", 
                    userId, bucket.getLimit(), bucket.getRemaining());
            }
            
            return !allowed;
        } catch (Exception e) {
            logger.error("Error checking user rate limit for user {}", userId, e);
            return false; // Allow on error
        }
    }
    
    public Map<String, Object> getRateLimitStatus(String userId) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // User rate limit status
            String userKey = "user:" + userId;
            RateLimitBucket userBucket = userBuckets.get(userKey);
            
            if (userBucket != null) {
                status.put("userLimit", userBucket.getLimit());
                status.put("userRemaining", userBucket.getRemaining());
                status.put("userResetTime", userBucket.getResetTime().toString());
            } else {
                int userLimit = 100; // Default limit
                status.put("userLimit", userLimit);
                status.put("userRemaining", userLimit);
                status.put("userResetTime", Instant.now().plus(1, ChronoUnit.MINUTES).toString());
            }
            
            status.put("userId", userId);
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status for user {}", userId, e);
            status.put("error", "Failed to get rate limit status");
        }
        
        return status;
    }
    
    public void clearUserRateLimit(String userId) {
        String key = "user:" + userId;
        userBuckets.remove(key);
        logger.info("Cleared rate limit for user {}", userId);
    }
    
    public void clearAllRateLimits() {
        userBuckets.clear();
        logger.info("Cleared all rate limits");
    }
    
    public void cleanupExpiredBuckets() {
        Instant now = Instant.now();
        
        // Clean up expired user buckets
        userBuckets.entrySet().removeIf(entry -> {
            RateLimitBucket bucket = entry.getValue();
            return now.isAfter(bucket.getResetTime());
        });
        
        logger.debug("Cleaned up expired rate limit buckets");
    }
}
