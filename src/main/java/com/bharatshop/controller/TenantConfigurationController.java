package com.bharatshop.controller;

import com.bharatshop.entity.TenantConfiguration;
import com.bharatshop.service.TenantConfigurationService;
import com.bharatshop.service.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tenant")
public class TenantConfigurationController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantConfigurationController.class);
    
    @Autowired
    private TenantConfigurationService tenantConfigurationService;
    
    @GetMapping("/config/{configKey}")
    public ResponseEntity<Map<String, Object>> getConfiguration(
            @PathVariable String configKey,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            Optional<TenantConfiguration> config = tenantConfigurationService.getConfiguration(tenantId, configKey);
            
            if (config.isPresent()) {
                TenantConfiguration configuration = config.get();
                Map<String, Object> response = new HashMap<>();
                response.put("key", configuration.getConfigKey());
                response.put("value", configuration.getTypedValue());
                response.put("type", configuration.getConfigType());
                response.put("description", configuration.getDescription());
                response.put("isFeatureFlag", configuration.getIsFeatureFlag());
                response.put("updatedAt", configuration.getUpdatedAt());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Configuration not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting configuration for tenant {} and key {}", tenantId, configKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get configuration"));
        }
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAllConfigurations(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            List<TenantConfiguration> configurations = tenantConfigurationService.getAllConfigurations(tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("configurations", configurations);
            response.put("count", configurations.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all configurations for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get configurations"));
        }
    }
    
    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> getAllFeatures(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            Map<String, Boolean> features = tenantConfigurationService.getAllFeatures(tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("features", features);
            response.put("count", features.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all features for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get features"));
        }
    }
    
    @GetMapping("/features/{featureKey}")
    public ResponseEntity<Map<String, Object>> isFeatureEnabled(
            @PathVariable String featureKey,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            boolean enabled = tenantConfigurationService.isFeatureEnabled(tenantId, featureKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("feature", featureKey);
            response.put("enabled", enabled);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking feature {} for tenant {}", featureKey, tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check feature"));
        }
    }
    
    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> createConfiguration(
            @RequestBody Map<String, Object> configRequest,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            String configKey = (String) configRequest.get("key");
            String configValue = (String) configRequest.get("value");
            String configType = (String) configRequest.getOrDefault("type", "STRING");
            String description = (String) configRequest.get("description");
            
            if (configKey == null || configValue == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Key and value are required"));
            }
            
            TenantConfiguration configuration = tenantConfigurationService.createConfiguration(
                tenantId, configKey, configValue, configType, description
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Configuration created successfully");
            response.put("configuration", configuration);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating configuration for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create configuration"));
        }
    }
    
    @PutMapping("/config/{configKey}")
    public ResponseEntity<Map<String, Object>> updateConfiguration(
            @PathVariable String configKey,
            @RequestBody Map<String, Object> configRequest,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            Optional<TenantConfiguration> existingConfig = tenantConfigurationService.getConfiguration(tenantId, configKey);
            if (existingConfig.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Configuration not found"));
            }
            
            String configValue = (String) configRequest.get("value");
            String description = (String) configRequest.get("description");
            
            if (configValue == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Value is required"));
            }
            
            TenantConfiguration configuration = existingConfig.get();
            configuration.setConfigValue(configValue);
            if (description != null) {
                configuration.setDescription(description);
            }
            
            TenantConfiguration updated = tenantConfigurationService.saveConfiguration(configuration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Configuration updated successfully");
            response.put("configuration", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating configuration for tenant {} and key {}", tenantId, configKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update configuration"));
        }
    }
    
    @DeleteMapping("/config/{configKey}")
    public ResponseEntity<Map<String, Object>> deleteConfiguration(
            @PathVariable String configKey,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            boolean deleted = tenantConfigurationService.deleteConfiguration(tenantId, configKey);
            
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Configuration deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Configuration not found"));
            }
        } catch (Exception e) {
            logger.error("Error deleting configuration for tenant {} and key {}", tenantId, configKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete configuration"));
        }
    }
    
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            tenantConfigurationService.clearCache(tenantId);
            return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
        } catch (Exception e) {
            logger.error("Error clearing cache for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to clear cache"));
        }
    }
    
    @GetMapping("/business-rules")
    public ResponseEntity<Map<String, Object>> getBusinessRules(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        try {
            Map<String, Object> rules = new HashMap<>();
            
            // Order rules
            rules.put("orderAcceptanceTimeout", tenantConfigurationService.getOrderAcceptanceTimeout(tenantId));
            rules.put("orderPreparationTimeout", tenantConfigurationService.getOrderPreparationTimeout(tenantId));
            rules.put("deliveryRadius", tenantConfigurationService.getDeliveryRadius(tenantId));
            rules.put("minimumOrderAmount", tenantConfigurationService.getMinimumOrderAmount(tenantId));
            rules.put("maximumOrderAmount", tenantConfigurationService.getMaximumOrderAmount(tenantId));
            
            // Inventory rules
            rules.put("inventoryReservationTimeout", tenantConfigurationService.getInventoryReservationTimeout(tenantId));
            
            // OTP rules
            rules.put("otpExpiryMinutes", tenantConfigurationService.getOtpExpiryMinutes(tenantId));
            rules.put("otpMaxAttempts", tenantConfigurationService.getOtpMaxAttempts(tenantId));
            
            // Rate limiting
            rules.put("rateLimitPerUser", tenantConfigurationService.getRateLimitPerUser(tenantId));
            rules.put("rateLimitPerTenant", tenantConfigurationService.getRateLimitPerTenant(tenantId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("businessRules", rules);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting business rules for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get business rules"));
        }
    }
}