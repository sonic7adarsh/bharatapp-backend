package com.bharatshop.dto.seller;

import java.util.List;

public class SellerOrderDetail {
    private String id;
    private String status;
    private String createdAt;
    private Double total;
    private String paymentMethod;
    private CustomerContact customerContact;
    private Address address;
    private List<Item> items;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public CustomerContact getCustomerContact() { return customerContact; }
    public void setCustomerContact(CustomerContact customerContact) { this.customerContact = customerContact; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class CustomerContact {
        private String name;
        private String phone;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class Address {
        private String fullAddress;
        private String line1;
        private String city;
        private String zip;

        public String getFullAddress() { return fullAddress; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

        public String getLine1() { return line1; }
        public void setLine1(String line1) { this.line1 = line1; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getZip() { return zip; }
        public void setZip(String zip) { this.zip = zip; }
    }

    public static class Item {
        private String name;
        private Integer quantity;
        private Double price;
        private List<String> images;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
    }
}
