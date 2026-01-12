package com.bharatshop.service;

/**
 * Event triggered when order is delivered
 */
public class OrderDeliveredEvent extends OrderEvent {
    
    public OrderDeliveredEvent(String tenantId, String userId, String orderId, String storeId) {
        super(tenantId, userId, orderId, storeId);
    }
}