package com.bharatshop.dto.seller;

import java.util.List;

public class SellerOrderSummary {
    private String id;
    private String reference;
    private String status;
    private String createdAt;
    private Double total;
    private CustomerContact customerContact;
    private Integer itemsCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public CustomerContact getCustomerContact() { return customerContact; }
    public void setCustomerContact(CustomerContact customerContact) { this.customerContact = customerContact; }

    public Integer getItemsCount() { return itemsCount; }
    public void setItemsCount(Integer itemsCount) { this.itemsCount = itemsCount; }

    public static class CustomerContact {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
