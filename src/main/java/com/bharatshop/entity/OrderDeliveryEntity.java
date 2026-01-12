package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_deliveries")
public class OrderDeliveryEntity {
    @Id
    private String id;
    private String tenantId;
    private String orderId;
    private String storeId;
    private String riderId;
    private String status; // PENDING|RIDER_ASSIGNED|PICKED_UP|OUT_FOR_DELIVERY|DELIVERED|FAILED
    private String otp;
    private Instant assignedAt;
    private Instant pickedUpAt;
    private Instant completedAt;
    private String failureReason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public Instant getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(Instant pickedUpAt) { this.pickedUpAt = pickedUpAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}