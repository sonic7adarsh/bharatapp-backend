package com.bharatshop.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    @Column(name = "id", length = 255)
    private String id;
    
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;
    
    @Column(name = "user_id", length = 255)
    private String userId;
    
    @Column(name = "user_email", length = 255)
    private String userEmail;
    
    @Column(name = "user_role", length = 100)
    private String userRole;
    
    @Column(name = "action", length = 255, nullable = false)
    private String action;
    
    @Column(name = "resource_type", length = 100)
    private String resourceType;
    
    @Column(name = "resource_id", length = 255)
    private String resourceId;
    
    @Column(name = "action_status", length = 50, nullable = false)
    private String actionStatus;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "request_id", length = 255)
    private String requestId;
    
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;
    
    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;
    
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AuditLog() {}

    public AuditLog(String id, String tenantId, String userId, String action, String actionStatus) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.action = action;
        this.actionStatus = actionStatus;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }

    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }

    public String getChanges() { return changes; }
    public void setChanges(String changes) { this.changes = changes; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum ActionStatus {
        SUCCESS("SUCCESS"),
        FAILURE("FAILURE"),
        PARTIAL_SUCCESS("PARTIAL_SUCCESS"),
        UNAUTHORIZED("UNAUTHORIZED"),
        FORBIDDEN("FORBIDDEN"),
        VALIDATION_ERROR("VALIDATION_ERROR");
        
        private final String value;
        
        ActionStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }

    public enum ActionType {
        // User actions
        USER_LOGIN("USER_LOGIN"),
        USER_LOGOUT("USER_LOGOUT"),
        USER_REGISTER("USER_REGISTER"),
        USER_UPDATE("USER_UPDATE"),
        USER_ROLE_SWITCH("USER_ROLE_SWITCH"),
        
        // Order actions
        ORDER_CREATE("ORDER_CREATE"),
        ORDER_UPDATE("ORDER_UPDATE"),
        ORDER_CANCEL("ORDER_CANCEL"),
        ORDER_ACCEPT("ORDER_ACCEPT"),
        ORDER_REJECT("ORDER_REJECT"),
        ORDER_SHIP("ORDER_SHIP"),
        ORDER_DELIVER("ORDER_DELIVER"),
        ORDER_REFUND("ORDER_REFUND"),
        
        // Product actions
        PRODUCT_CREATE("PRODUCT_CREATE"),
        PRODUCT_UPDATE("PRODUCT_UPDATE"),
        PRODUCT_DELETE("PRODUCT_DELETE"),
        PRODUCT_BULK_UPLOAD("PRODUCT_BULK_UPLOAD"),
        
        // Store actions
        STORE_CREATE("STORE_CREATE"),
        STORE_UPDATE("STORE_UPDATE"),
        STORE_DELETE("STORE_DELETE"),
        STORE_STATUS_CHANGE("STORE_STATUS_CHANGE"),
        
        // Inventory actions
        INVENTORY_RESERVE("INVENTORY_RESERVE"),
        INVENTORY_RELEASE("INVENTORY_RELEASE"),
        INVENTORY_UPDATE("INVENTORY_UPDATE"),
        
        // Payment actions
        PAYMENT_INITIATE("PAYMENT_INITIATE"),
        PAYMENT_COMPLETE("PAYMENT_COMPLETE"),
        PAYMENT_FAILED("PAYMENT_FAILED"),
        PAYMENT_REFUND("PAYMENT_REFUND"),
        
        // Notification actions
        NOTIFICATION_SEND("NOTIFICATION_SEND"),
        NOTIFICATION_FAILED("NOTIFICATION_FAILED"),
        
        // System actions
        SYSTEM_STARTUP("SYSTEM_STARTUP"),
        SYSTEM_SHUTDOWN("SYSTEM_SHUTDOWN"),
        CONFIG_UPDATE("CONFIG_UPDATE"),
        
        // Security actions
        SECURITY_VIOLATION("SECURITY_VIOLATION"),
        RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED"),
        UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS");
        
        private final String value;
        
        ActionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}