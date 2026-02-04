package com.bharatshop.service;

/**
 * Event triggered when a store accepts an order
 */
public class OrderAcceptedEvent extends OrderEvent {
    
    public OrderAcceptedEvent(String userId, String orderId, String storeId) {
        super(userId, orderId, storeId);
    }
}