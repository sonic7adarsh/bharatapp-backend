package com.bharatshop.service;

/**
 * Event triggered when a new order is placed
 */
public class OrderPlacedEvent extends OrderEvent {
    private double orderTotal;
    
    public OrderPlacedEvent(String userId, String orderId, String storeId, double orderTotal) {
        super(userId, orderId, storeId);
        this.orderTotal = orderTotal;
    }
    
    public double getOrderTotal() { return orderTotal; }
}