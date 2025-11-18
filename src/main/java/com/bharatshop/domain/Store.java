package com.bharatshop.domain;

import java.util.List;

public class Store {
    private String id;
    private String name;
    private String area;
    private String category;
    private String ownerId;
    private String ownerPhone;
    private List<Product> products;
    private String status; // open, closed
    private Boolean orderingDisabled;
    private String closedReason;
    private java.time.Instant closedUntil;
    private java.time.Instant updatedAt;

    public Store() {}

    public Store(String id, String name, String area, String category) {
        this.id = id; this.name = name; this.area = area; this.category = category;
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
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getOrderingDisabled() { return orderingDisabled; }
    public void setOrderingDisabled(Boolean orderingDisabled) { this.orderingDisabled = orderingDisabled; }
    public String getClosedReason() { return closedReason; }
    public void setClosedReason(String closedReason) { this.closedReason = closedReason; }
    public java.time.Instant getClosedUntil() { return closedUntil; }
    public void setClosedUntil(java.time.Instant closedUntil) { this.closedUntil = closedUntil; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
}