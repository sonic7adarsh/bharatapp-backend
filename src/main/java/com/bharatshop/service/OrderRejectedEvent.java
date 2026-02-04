package com.bharatshop.service;

/**
 * Event triggered when a store rejects an order
 */
public class OrderRejectedEvent extends OrderEvent {
    private String reason;
    
    public OrderRejectedEvent(String userId, String orderId, String storeId, String reason) {
        super(userId, orderId, storeId);
        this.reason = reason;
    }
    
    public String getReason() { return reason; }
}