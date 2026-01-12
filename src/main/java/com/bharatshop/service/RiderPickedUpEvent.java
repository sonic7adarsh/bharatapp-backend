package com.bharatshop.service;

/**
 * Event triggered when a rider picks up an order
 */
public class RiderPickedUpEvent extends OrderEvent {
    private String riderId;
    
    public RiderPickedUpEvent(String tenantId, String userId, String orderId, String storeId, String riderId) {
        super(tenantId, userId, orderId, storeId);
        this.riderId = riderId;
    }
    
    public String getRiderId() { return riderId; }
}