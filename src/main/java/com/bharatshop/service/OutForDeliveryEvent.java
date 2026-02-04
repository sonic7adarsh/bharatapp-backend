package com.bharatshop.service;

/**
 * Event triggered when order is out for delivery
 */
public class OutForDeliveryEvent extends OrderEvent {
    private String riderId;
    private String deliveryAddress;
    
    public OutForDeliveryEvent(String userId, String orderId, String storeId, String riderId, String deliveryAddress) {
        super(userId, orderId, storeId);
        this.riderId = riderId;
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getRiderId() { return riderId; }
    public String getDeliveryAddress() { return deliveryAddress; }
}