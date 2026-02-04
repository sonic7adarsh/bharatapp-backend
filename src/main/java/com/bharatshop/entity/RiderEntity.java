package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "riders")
public class RiderEntity {
    @Id
    private String id;
    private String name;
    private String phone;
    private String status; // OFFLINE|ONLINE (availability only)
    private String currentZoneId;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;

    @PrePersist public void onCreate(){ var now = java.time.Instant.now(); if(createdAt==null) createdAt=now; updatedAt=now; }
    @PreUpdate public void onUpdate(){ updatedAt = java.time.Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // tenant getters/setters removed
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentZoneId() { return currentZoneId; }
    public void setCurrentZoneId(String currentZoneId) { this.currentZoneId = currentZoneId; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
}