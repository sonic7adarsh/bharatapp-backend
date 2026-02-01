package com.bharatshop.domain;

public class Product {
    private String id;
    private String name;
    private double price;
    private String description;
    private String image;
    private String category;
    private String categoryId;
    private String storeId;
    private String currency;
    private String sku;
    private Integer stock;
    private Boolean active;

    public Product() {}

    public Product(String id, String name, double price, String category, String storeId) {
        this.id = id; this.name = name; this.price = price; this.category = category; this.storeId = storeId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}