package com.bharatshop.domain;

public class PaymentOrder {
    private String id;
    private int amount; // paise

    public PaymentOrder() {}
    public PaymentOrder(String id, int amount) { this.id = id; this.amount = amount; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}