package com.bharatshop.service;

/**
 * Event triggered when order is delivered
 */
public class OrderDeliveredEvent extends OrderEvent {
    
    public OrderDeliveredEvent(String userId, String orderId, String storeId) {
        super(userId, orderId, storeId);
    }
}