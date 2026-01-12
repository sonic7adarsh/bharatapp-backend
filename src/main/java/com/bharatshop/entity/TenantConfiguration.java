package com.bharatshop.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_configurations")
public class TenantConfiguration {
    
    @Id
    @Column(name = "id", length = 255)
    private String id;
    
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;
    
    @Column(name = "config_key", length = 255, nullable = false)
    private String configKey;
    
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    @Column(name = "config_type", length = 50)
    private String configType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_encrypted")
    private Boolean isEncrypted;
    
    @Column(name = "is_feature_flag")
    private Boolean isFeatureFlag;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TenantConfiguration() {}

    public TenantConfiguration(String id, String tenantId, String configKey, String configValue, String configType, String description) {
        this.id = id;
        this.tenantId = tenantId;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.description = description;
        this.isEncrypted = false;
        this.isFeatureFlag = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getConfigType() { return configType; }
    public void setConfigType(String configType) { this.configType = configType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }

    public Boolean getIsFeatureFlag() { return isFeatureFlag; }
    public void setIsFeatureFlag(Boolean isFeatureFlag) { this.isFeatureFlag = isFeatureFlag; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Object getTypedValue() {
        if (configValue == null) return null;
        
        try {
            switch (configType) {
                case "BOOLEAN":
                    return Boolean.parseBoolean(configValue);
                case "INTEGER":
                    return Integer.parseInt(configValue);
                case "LONG":
                    return Long.parseLong(configValue);
                case "DOUBLE":
                    return Double.parseDouble(configValue);
                case "STRING":
                default:
                    return configValue;
            }
        } catch (Exception e) {
            return configValue;
        }
    }

    public boolean isFeatureEnabled() {
        return isFeatureFlag != null && isFeatureFlag && Boolean.parseBoolean(configValue);
    }
}