package com.bharatshop.service;

/**
 * Event triggered when order is cancelled
 */
public class OrderCancelledEvent extends OrderEvent {
    private String reason;
    
    public OrderCancelledEvent(String userId, String orderId, String storeId, String reason) {
        super(userId, orderId, storeId);
        this.reason = reason;
    }
    
    public String getReason() { return reason; }
}