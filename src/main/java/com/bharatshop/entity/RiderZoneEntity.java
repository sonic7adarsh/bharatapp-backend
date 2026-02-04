package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rider_zones")
public class RiderZoneEntity {
    @Id
    private String id;
    // tenantId removed
    private String riderId;
    private String zoneId;
    private java.time.Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // tenant getters/setters removed
    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}