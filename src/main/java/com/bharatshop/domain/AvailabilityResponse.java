package com.bharatshop.domain;

public class AvailabilityResponse {
    private boolean available;
    private String reason;
    private Integer nights;
    private Integer base;
    private Double surchargeRate;
    private Integer rooms;
    private Integer perRoomMax;
    private Boolean extraMattressAllowed;
    private Integer extraMattressCount;
    private Integer mattressFeePerNight;
    private Integer subtotal;
    private Integer taxes;
    private Integer fees;
    private Integer total;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public Integer getBase() { return base; }
    public void setBase(Integer base) { this.base = base; }
    public Double getSurchargeRate() { return surchargeRate; }
    public void setSurchargeRate(Double surchargeRate) { this.surchargeRate = surchargeRate; }
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
    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
    public Integer getTaxes() { return taxes; }
    public void setTaxes(Integer taxes) { this.taxes = taxes; }
    public Integer getFees() { return fees; }
    public void setFees(Integer fees) { this.fees = fees; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}