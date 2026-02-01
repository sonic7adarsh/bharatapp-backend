package com.bharatshop.domain;

public class BookingDetails {
    private String roomId;
    private String checkIn;
    private String checkOut;
    private int guests;
    private int nights;
    private Integer rooms;
    private Integer perRoomMax;
    private Boolean extraMattressAllowed;
    private Integer extraMattressCount;
    private Integer mattressFeePerNight;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
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