package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "zones")
public class ZoneEntity {
    @Id
    private String id;
    private String tenantId;
    private String name;
    private String type; // radius|polygon
    private Double centerLat;
    private Double centerLng;
    private Integer radiusMeters;
    private String polygonJson; // optional; array of {lat,lng}
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;

    @PrePersist public void onCreate(){ var now = java.time.Instant.now(); if(createdAt==null) createdAt=now; updatedAt=now; }
    @PreUpdate public void onUpdate(){ updatedAt = java.time.Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public Integer getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(Integer radiusMeters) { this.radiusMeters = radiusMeters; }
    public String getPolygonJson() { return polygonJson; }
    public void setPolygonJson(String polygonJson) { this.polygonJson = polygonJson; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
}