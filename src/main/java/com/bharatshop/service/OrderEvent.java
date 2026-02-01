package com.bharatshop.service;

/**
 * Base class for all order-related events
 */
public abstract class OrderEvent {
    protected String tenantId;
    protected String userId;
    protected String orderId;
    protected String storeId;
    
    public OrderEvent(String tenantId, String userId, String orderId, String storeId) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.orderId = orderId;
        this.storeId = storeId;
    }
    
    public String getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public String getStoreId() { return storeId; }
}