package com.bharatshop.service;

/**
 * Event triggered when a rider is assigned to an order
 */
public class RiderAssignedEvent extends OrderEvent {
    private String riderId;
    private String estimatedTime;
    
    public RiderAssignedEvent(String userId, String orderId, String storeId, String riderId, String estimatedTime) {
        super(userId, orderId, storeId);
        this.riderId = riderId;
        this.estimatedTime = estimatedTime;
    }
    
    public String getRiderId() { return riderId; }
    public String getEstimatedTime() { return estimatedTime; }
}