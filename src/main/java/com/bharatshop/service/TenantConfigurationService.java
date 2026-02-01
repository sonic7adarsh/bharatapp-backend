package com.bharatshop.service;

import com.bharatshop.entity.TenantConfiguration;
import com.bharatshop.repository.TenantConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantConfigurationService.class);
    
    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;
    
    private final Map<String, Map<String, TenantConfiguration>> cache = new HashMap<>();
    
    public Optional<TenantConfiguration> getConfiguration(String tenantId, String configKey) {
        try {
            // Check cache first
            Map<String, TenantConfiguration> tenantCache = cache.get(tenantId);
            if (tenantCache != null && tenantCache.containsKey(configKey)) {
                return Optional.of(tenantCache.get(configKey));
            }
            
            // Fetch from database
            Optional<TenantConfiguration> config = tenantConfigurationRepository.findByTenantIdAndConfigKey(tenantId, configKey);
            
            // Cache the result
            if (config.isPresent()) {
                cache.computeIfAbsent(tenantId, k -> new HashMap<>()).put(configKey, config.get());
            }
            
            return config;
        } catch (Exception e) {
            logger.error("Error getting configuration for tenant {} and key {}", tenantId, configKey, e);
            return Optional.empty();
        }
    }
    
    public String getStringValue(String tenantId, String configKey, String defaultValue) {
        return getConfiguration(tenantId, configKey)
            .map(TenantConfiguration::getConfigValue)
            .orElse(defaultValue);
    }
    
    public boolean getBooleanValue(String tenantId, String configKey, boolean defaultValue) {
        return getConfiguration(tenantId, configKey)
            .map(config -> Boolean.parseBoolean(config.getConfigValue()))
            .orElse(defaultValue);
    }
    
    public int getIntValue(String tenantId, String configKey, int defaultValue) {
        return getConfiguration(tenantId, configKey)
            .map(config -> {
                try {
                    return Integer.parseInt(config.getConfigValue());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid integer value for config {}: {}", configKey, config.getConfigValue());
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }
    
    public long getLongValue(String tenantId, String configKey, long defaultValue) {
        return getConfiguration(tenantId, configKey)
            .map(config -> {
                try {
                    return Long.parseLong(config.getConfigValue());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid long value for config {}: {}", configKey, config.getConfigValue());
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }
    
    public double getDoubleValue(String tenantId, String configKey, double defaultValue) {
        return getConfiguration(tenantId, configKey)
            .map(config -> {
                try {
                    return Double.parseDouble(config.getConfigValue());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid double value for config {}: {}", configKey, config.getConfigValue());
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }
    
    public boolean isFeatureEnabled(String tenantId, String featureKey) {
        String configKey = "feature." + featureKey + ".enabled";
        return getBooleanValue(tenantId, configKey, true); // Default to enabled
    }
    
    public Map<String, Boolean> getAllFeatures(String tenantId) {
        try {
            List<TenantConfiguration> features = tenantConfigurationRepository.findByTenantIdAndIsFeatureFlagTrue(tenantId);
            return features.stream()
                .collect(Collectors.toMap(
                    TenantConfiguration::getConfigKey,
                    config -> Boolean.parseBoolean(config.getConfigValue())
                ));
        } catch (Exception e) {
            logger.error("Error getting all features for tenant {}", tenantId, e);
            return new HashMap<>();
        }
    }
    
    public List<TenantConfiguration> getAllConfigurations(String tenantId) {
        try {
            return tenantConfigurationRepository.findByTenantId(tenantId);
        } catch (Exception e) {
            logger.error("Error getting all configurations for tenant {}", tenantId, e);
            return new ArrayList<>();
        }
    }
    
    public TenantConfiguration saveConfiguration(TenantConfiguration configuration) {
        try {
            TenantConfiguration saved = tenantConfigurationRepository.save(configuration);
            
            // Update cache
            cache.computeIfAbsent(configuration.getTenantId(), k -> new HashMap<>())
                .put(configuration.getConfigKey(), saved);
            
            return saved;
        } catch (Exception e) {
            logger.error("Error saving configuration: {}", configuration, e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
    
    public TenantConfiguration createConfiguration(String tenantId, String configKey, String configValue, String configType, String description) {
        TenantConfiguration config = new TenantConfiguration(
            UUID.randomUUID().toString(),
            tenantId,
            configKey,
            configValue,
            configType,
            description
        );
        return saveConfiguration(config);
    }
    
    public boolean deleteConfiguration(String tenantId, String configKey) {
        try {
            tenantConfigurationRepository.deleteByTenantIdAndConfigKey(tenantId, configKey);
            
            // Remove from cache
            Map<String, TenantConfiguration> tenantCache = cache.get(tenantId);
            if (tenantCache != null) {
                tenantCache.remove(configKey);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error deleting configuration for tenant {} and key {}", tenantId, configKey, e);
            return false;
        }
    }
    
    public void clearCache(String tenantId) {
        if (tenantId != null) {
            cache.remove(tenantId);
        } else {
            cache.clear();
        }
    }
    
    public void clearAllCache() {
        cache.clear();
    }
    
    // Business rule helpers
    public int getOrderAcceptanceTimeout(String tenantId) {
        return getIntValue(tenantId, "order.acceptance.timeout", 15); // 15 minutes default
    }
    
    public int getOrderPreparationTimeout(String tenantId) {
        return getIntValue(tenantId, "order.preparation.timeout", 30); // 30 minutes default
    }
    
    public double getDeliveryRadius(String tenantId) {
        return getDoubleValue(tenantId, "order.delivery.radius", 5.0); // 5 km default
    }
    
    public int getMinimumOrderAmount(String tenantId) {
        return getIntValue(tenantId, "order.minimum.amount", 100); // ₹100 default
    }
    
    public int getMaximumOrderAmount(String tenantId) {
        return getIntValue(tenantId, "order.maximum.amount", 50000); // ₹50,000 default
    }
    
    public int getInventoryReservationTimeout(String tenantId) {
        return getIntValue(tenantId, "inventory.reservation.timeout", 10); // 10 minutes default
    }
    
    public int getOtpExpiryMinutes(String tenantId) {
        return getIntValue(tenantId, "otp.expiry.minutes", 5); // 5 minutes default
    }
    
    public int getOtpMaxAttempts(String tenantId) {
        return getIntValue(tenantId, "otp.max.attempts", 3); // 3 attempts default
    }
    
    public int getRateLimitPerUser(String tenantId) {
        return getIntValue(tenantId, "rate.limit.per.user", 100); // 100 requests per minute
    }
    
    public int getRateLimitPerTenant(String tenantId) {
        return getIntValue(tenantId, "rate.limit.per.tenant", 1000); // 1000 requests per minute
    }
}