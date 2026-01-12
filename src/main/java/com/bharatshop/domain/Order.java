package com.bharatshop.domain;

import java.util.List;

public class Order {
    private String id;
    private String reference;
    private String status; // placed|processing|delivered|cancelled
    private Double total;
    private Totals totals;
    private List<CartItem> items;
    private String paymentMethod; // cod|online
    private PaymentInfo paymentInfo;
    private Address address;
    private String deliverySlot;
    private String deliveryInstructions;

    private String createdAt;
    private String sellerResponseDeadline;
    private String sellerAcceptedAt;
    private String cancelledAt;
    private String cancellationReason;
    private String storeId;
    private String notes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public Totals getTotals() { return totals; }
    public void setTotals(Totals totals) { this.totals = totals; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentInfo getPaymentInfo() { return paymentInfo; }
    public void setPaymentInfo(PaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
    public String getDeliverySlot() { return deliverySlot; }
    public void setDeliverySlot(String deliverySlot) { this.deliverySlot = deliverySlot; }
    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String deliveryInstructions) { this.deliveryInstructions = deliveryInstructions; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getSellerResponseDeadline() { return sellerResponseDeadline; }
    public void setSellerResponseDeadline(String sellerResponseDeadline) { this.sellerResponseDeadline = sellerResponseDeadline; }
    public String getSellerAcceptedAt() { return sellerAcceptedAt; }
    public void setSellerAcceptedAt(String sellerAcceptedAt) { this.sellerAcceptedAt = sellerAcceptedAt; }
    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public static class Totals {
        public Double subtotal;
        public Double deliveryFee;
        public Double tax;
        public Double tip;
        public Double payable;
    }

    public static class PaymentInfo {
        public String gateway; // razorpay|mock
        public String orderId;
        public String paymentId;
        public String transactionId;
        public String status; // success|failed
    }
}