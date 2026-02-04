package com.bharatshop.dto;

import java.util.List;

public class CheckoutRequest {
    private List<CheckoutItem> items;
    private String paymentMethod; // cod|online
    // Optional fields to align with frontend payload; not strictly required by service
    private String storeId;
    private String deliveryAddressId;
    private String deliveryAddress; // Raw address string
    private String addressId; // Optional: ID of saved address
    private String customerName;
    private String customerPhone;
    private String customerAlternatePhone;
    private PaymentInfo paymentInfo;
    private String prescriptionUrl;
    private String deliverySlot;
    private String notes; // Add notes field

    public List<CheckoutItem> getItems() { return items; }
    public void setItems(List<CheckoutItem> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getDeliveryAddressId() { return deliveryAddressId; }
    public void setDeliveryAddressId(String deliveryAddressId) { this.deliveryAddressId = deliveryAddressId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(Object deliveryAddress) { 
        if (deliveryAddress instanceof String) {
            this.deliveryAddress = (String) deliveryAddress;
        } else {
             // Handle object case if needed, or ignore
             this.deliveryAddress = deliveryAddress != null ? deliveryAddress.toString() : null;
        }
    }
    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerAlternatePhone() { return customerAlternatePhone; }
    public void setCustomerAlternatePhone(String customerAlternatePhone) { this.customerAlternatePhone = customerAlternatePhone; }
    public PaymentInfo getPaymentInfo() { return paymentInfo; }
    public void setPaymentInfo(PaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }
    public String getPrescriptionUrl() { return prescriptionUrl; }
    public void setPrescriptionUrl(String prescriptionUrl) { this.prescriptionUrl = prescriptionUrl; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public static class PaymentInfo {
        public String gateway;
        public String orderId;
        public String paymentId;
        public String signature;
    }
}