package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "stores")
public class StoreEntity {
    @Id
    private String id;
    private String name;
    private String area;
    private String category;
    private String ownerId;
    private String ownerPhone;
    private String status; // open, closed
    private Boolean orderingDisabled;
    private String closedReason;
    private java.time.Instant closedUntil;
    private String logo; // filename or URL reference
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;

    @PrePersist
    public void onCreate() {
        java.time.Instant now = java.time.Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = java.time.Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getOrderingDisabled() { return orderingDisabled; }
    public void setOrderingDisabled(Boolean orderingDisabled) { this.orderingDisabled = orderingDisabled; }
    public String getClosedReason() { return closedReason; }
    public void setClosedReason(String closedReason) { this.closedReason = closedReason; }
    public java.time.Instant getClosedUntil() { return closedUntil; }
    public void setClosedUntil(java.time.Instant closedUntil) { this.closedUntil = closedUntil; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
}