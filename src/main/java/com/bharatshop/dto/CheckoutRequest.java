package com.bharatshop.dto;

import java.util.List;

public class CheckoutRequest {
    private List<CheckoutItem> items;
    private String paymentMethod; // cod|online

    public List<CheckoutItem> getItems() { return items; }
    public void setItems(List<CheckoutItem> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}