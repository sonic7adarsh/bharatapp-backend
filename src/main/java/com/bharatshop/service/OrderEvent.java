package com.bharatshop.service;

/**
 * Base class for all order-related events
 */
public abstract class OrderEvent {
    protected String userId;
    protected String orderId;
    protected String storeId;
    
    public OrderEvent(String userId, String orderId, String storeId) {
        this.userId = userId;
        this.orderId = orderId;
        this.storeId = storeId;
    }
    
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public String getStoreId() { return storeId; }
}