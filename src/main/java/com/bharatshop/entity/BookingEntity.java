package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class BookingEntity {
    @Id
    private String id;
    private String reference;
    private String userId;
    private String status;
    private Double total;
    private String paymentMethod;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant sellerResponseDeadline;
    private Instant sellerAcceptedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private String storeId;
    private String roomId;
    private String notes;

    // Booking details
    private String checkIn;   // ISO date string
    private String checkOut;  // ISO date string
    private Integer guests;
    private Integer nights;
    private Integer rooms;
    private Integer perRoomMax;
    private Boolean extraMattressAllowed;
    private Integer extraMattressCount;
    private Integer mattressFeePerNight;

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
    public Instant getSellerResponseDeadline() { return sellerResponseDeadline; }
    public void setSellerResponseDeadline(Instant sellerResponseDeadline) { this.sellerResponseDeadline = sellerResponseDeadline; }
    public Instant getSellerAcceptedAt() { return sellerAcceptedAt; }
    public void setSellerAcceptedAt(Instant sellerAcceptedAt) { this.sellerAcceptedAt = sellerAcceptedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public Integer getRooms() { return rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }
    public Integer getPerRoomMax() { return perRoomMax; }
    public void setPerRoomMax(Integer perRoomMax) { this.perRoomMax = perRoomMax; }
    public Boolean getExtraMattressAllowed() { return extraMattressAllowed; }
    public void setExtraMattressAllowed(Boolean extraMattressAllowed) { this.extraMattressAllowed = extraMattressAllowed; }
    public Integer getExtraMattressCount() { return extraMattressCount; }
    public void setExtraMattressCount(Integer extraMattressCount) { this.extraMattressCount = extraMattressCount; }
    public Integer getMattressFeePerNight() { return mattressFeePerNight; }
    public void setMattressFeePerNight(Integer mattressFeePerNight) { this.mattressFeePerNight = mattressFeePerNight; }
}