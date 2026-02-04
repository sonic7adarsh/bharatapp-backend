package com.bharatshop.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable ledger of all money movements.
 * Enforces ACID properties by being an append-only log of financial events.
 */
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    private String id;

    private String paymentId; // Link to PaymentEntity
    private String orderId;   // Link to OrderEntity
    
    // Type: PAYMENT, REFUND, PAYOUT, SETTLEMENT
    private String type;
    
    private Double amount;
    private String currency;
    
    // Status: SUCCESS, FAILED, PENDING
    private String status;
    
    // External reference (e.g., refund_id from Razorpay)
    private String referenceId;
    
    private String description;
    
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
