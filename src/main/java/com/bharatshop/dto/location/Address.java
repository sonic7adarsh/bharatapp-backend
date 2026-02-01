package com.bharatshop.dto.location;

public class Address {
    private String suburb;
    private String neighbourhood;
    private String residential;
    private String city;
    private String town;
    private String state;
    private String postcode;

    public String getSuburb() { return suburb; }
    public void setSuburb(String suburb) { this.suburb = suburb; }

    public String getNeighbourhood() { return neighbourhood; }
    public void setNeighbourhood(String neighbourhood) { this.neighbourhood = neighbourhood; }

    public String getResidential() { return residential; }
    public void setResidential(String residential) { this.residential = residential; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }
}
