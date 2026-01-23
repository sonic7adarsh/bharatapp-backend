package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "rider_status")
public class RiderStatusEntity {
    @Id
    private String id;
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Column(name = "rider_id", nullable = false)
    private String riderId;
    @Column(name = "status", nullable = false)
    private String status; // ONLINE|OFFLINE
    @Column(name = "ts", nullable = false)
    private java.time.Instant ts;

    @PrePersist public void onCreate(){ if(ts==null) ts = java.time.Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.time.Instant getTs() { return ts; }
    public void setTs(java.time.Instant ts) { this.ts = ts; }
}