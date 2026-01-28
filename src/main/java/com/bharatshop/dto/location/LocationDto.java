package com.bharatshop.dto.location;

public class LocationDto {
    private String locality;
    private String city;
    private String state;
    private String pincode;

    public LocationDto() {}

    public LocationDto(String locality, String city, String state, String pincode) {
        this.locality = locality;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}
