package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttemptEntity {
    @Id
    private String id;

    private String deliveryId;
    private String status; // success|failed
    private String note;
    private Instant ts;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // tenant getters/setters removed
    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
}