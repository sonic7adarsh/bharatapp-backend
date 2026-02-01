package com.bharatshop.dto;

import java.util.List;

public class CheckoutRequest {
    private List<CheckoutItem> items;
    private String paymentMethod; // cod|online
    // Optional fields to align with frontend payload; not strictly required by service
    private String storeId;
    private String deliveryAddressId;
    private PaymentInfo paymentInfo;

    public List<CheckoutItem> getItems() { return items; }
    public void setItems(List<CheckoutItem> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getDeliveryAddressId() { return deliveryAddressId; }
    public void setDeliveryAddressId(String deliveryAddressId) { this.deliveryAddressId = deliveryAddressId; }
    public PaymentInfo getPaymentInfo() { return paymentInfo; }
    public void setPaymentInfo(PaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }

    public static class PaymentInfo {
        public String gateway;
        public String orderId;
        public String paymentId;
        public String signature;
    }
}