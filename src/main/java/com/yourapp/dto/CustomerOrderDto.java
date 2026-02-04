package com.yourapp.dto;

import java.util.List;

public class CustomerOrderDto {
    private String id;
    private String status;
    private Double total;
    private String createdAt;
    private Store store;
    private List<Item> items;

    public static class Store {
        private String id;
        private String name;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Item {
        private String productId;
        private String name;
        private Integer quantity;
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    private String deliverySlot;
    private String paymentMethod;
    private String type;
    private String prescriptionUrl;
    private Boolean isPrescriptionVerified;
    private ContactInfo sellerContact;
    private ContactInfo customerContact;
    private Address address;
    private Double distance;

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public static class ContactInfo {
        private String name;
        private String phone;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class Address {
        private String fullAddress;
        private String landmark;
        public String getFullAddress() { return fullAddress; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public String getLandmark() { return landmark; }
        public void setLandmark(String landmark) { this.landmark = landmark; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public String getDeliverySlot() { return deliverySlot; }
    public void setDeliverySlot(String deliverySlot) { this.deliverySlot = deliverySlot; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPrescriptionUrl() { return prescriptionUrl; }
    public void setPrescriptionUrl(String prescriptionUrl) { this.prescriptionUrl = prescriptionUrl; }
    public Boolean getIsPrescriptionVerified() { return isPrescriptionVerified; }
    public void setIsPrescriptionVerified(Boolean isPrescriptionVerified) { this.isPrescriptionVerified = isPrescriptionVerified; }
    public ContactInfo getSellerContact() { return sellerContact; }
    public void setSellerContact(ContactInfo sellerContact) { this.sellerContact = sellerContact; }
    public ContactInfo getCustomerContact() { return customerContact; }
    public void setCustomerContact(ContactInfo customerContact) { this.customerContact = customerContact; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}