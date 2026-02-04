package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

@Entity
@Table(name = "order_deliveries")
public class OrderDeliveryEntity {

  @Id
  @Column(name = "delivery_id", nullable = false, updatable = false)
  private String deliveryId;

  @Column(name = "order_id", nullable = false)
  private String orderId;

  @Column(name = "store_id")
  private String storeId;

  @Column(name = "rider_id")
  private String riderId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "assigned_at")
  private Instant assignedAt;

  @Column(name = "picked_up_at")
  private Instant pickedUpAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "otp")
    private String otp;

    @Column(name = "otp_generated_at")
    private Instant otpGeneratedAt;

    @Column(name = "otp_sent_at")
    private Instant otpSentAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "failure_reason")
  private String failureReason;

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }

  public String getDeliveryId() { return deliveryId; }
  public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
  public String getOrderId() { return orderId; }
  public void setOrderId(String orderId) { this.orderId = orderId; }
  public String getStoreId() { return storeId; }
  public void setStoreId(String storeId) { this.storeId = storeId; }
  public String getRiderId() { return riderId; }
  public void setRiderId(String riderId) { this.riderId = riderId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getAssignedAt() { return assignedAt; }
  public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
  public Instant getPickedUpAt() { return pickedUpAt; }
  public void setPickedUpAt(Instant pickedUpAt) { this.pickedUpAt = pickedUpAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public String getOtp() { return otp; }
  public void setOtp(String otp) { this.otp = otp; }
  public Instant getOtpGeneratedAt() { return otpGeneratedAt; }
  public void setOtpGeneratedAt(Instant otpGeneratedAt) { this.otpGeneratedAt = otpGeneratedAt; }
  public Instant getOtpSentAt() { return otpSentAt; }
  public void setOtpSentAt(Instant otpSentAt) { this.otpSentAt = otpSentAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}