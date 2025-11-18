package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

@Entity
@Table(name = "rooms")
public class RoomEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String storeId; // Hotel mapped to StoreEntity

    private String name;
    private String description;
    private String image;

    @Column(name = "price_per_night")
    private Double pricePerNight;

    private String currency;

    @Column(name = "per_room_max")
    private Integer perRoomMax;

    @Column(name = "extra_mattress_allowed")
    private Boolean extraMattressAllowed;

    private Boolean active;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (active == null) active = Boolean.TRUE;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Double pricePerNight) { this.pricePerNight = pricePerNight; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getPerRoomMax() { return perRoomMax; }
    public void setPerRoomMax(Integer perRoomMax) { this.perRoomMax = perRoomMax; }
    public Boolean getExtraMattressAllowed() { return extraMattressAllowed; }
    public void setExtraMattressAllowed(Boolean extraMattressAllowed) { this.extraMattressAllowed = extraMattressAllowed; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}