package com.bharatshop.service;

/**
 * Event triggered when a store accepts an order
 */
public class OrderAcceptedEvent extends OrderEvent {
    
    public OrderAcceptedEvent(String tenantId, String userId, String orderId, String storeId) {
        super(tenantId, userId, orderId, storeId);
    }
}