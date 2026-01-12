package com.bharatshop.service;

/**
 * Event triggered when a new order is placed
 */
public class OrderPlacedEvent extends OrderEvent {
    private double orderTotal;
    
    public OrderPlacedEvent(String tenantId, String userId, String orderId, String storeId, double orderTotal) {
        super(tenantId, userId, orderId, storeId);
        this.orderTotal = orderTotal;
    }
    
    public double getOrderTotal() { return orderTotal; }
}