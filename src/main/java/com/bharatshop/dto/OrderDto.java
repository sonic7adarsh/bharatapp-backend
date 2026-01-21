package com.bharatshop.dto;

import java.util.List;

public class OrderDto {
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
}