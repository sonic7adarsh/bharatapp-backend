package com.bharatshop.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    private String tenantId;
    private String reference;
    private String userId;
    private String status;
    private Double total;
    private String paymentMethod;
    private Instant createdAt;
    private Instant updatedAt;
    private String type;
    private Instant sellerResponseDeadline;
    private Instant sellerAcceptedAt;
    private Instant sellerRejectedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private String storeId;
    private String sellerId;
    private String notes;

    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items; }
    public Instant getSellerResponseDeadline() { return sellerResponseDeadline; }
    public void setSellerResponseDeadline(Instant sellerResponseDeadline) { this.sellerResponseDeadline = sellerResponseDeadline; }
    public Instant getSellerAcceptedAt() { return sellerAcceptedAt; }
    public void setSellerAcceptedAt(Instant sellerAcceptedAt) { this.sellerAcceptedAt = sellerAcceptedAt; }
    public Instant getSellerRejectedAt() { return sellerRejectedAt; }
    public void setSellerRejectedAt(Instant sellerRejectedAt) { this.sellerRejectedAt = sellerRejectedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}