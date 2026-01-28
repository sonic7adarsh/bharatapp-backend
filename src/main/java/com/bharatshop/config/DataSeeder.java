package com.bharatshop.config;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.repository.CategoryRepository;
import com.bharatshop.service.ProductService;
import com.bharatshop.service.StoreService;
import com.bharatshop.tenant.TenantContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    private final StoreService storeService;
    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    public DataSeeder(StoreService storeService, ProductService productService, CategoryRepository categoryRepository) {
        this.storeService = storeService;
        this.productService = productService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        TenantContext.setTenant("tenantA");
        
        // Seed Categories
        createCategory("cat-grocery", "Grocery", "grocery", 1);
        createCategory("cat-pharmacy", "Pharmacy", "pharmacy", 2);
        createCategory("cat-electronics", "Electronics", "electronics", 3);
        createCategory("cat-fashion", "Fashion", "fashion", 4);

        // Seed Stores
        Store s1 = new Store("store-fresh-mart", "Fresh Mart", "MG Road", "grocery");
        Store s2 = new Store("store-city-pharmacy", "City Pharmacy", "Brigade", "pharmacy");
        
        storeService.add(s1); 
        storeService.add(s2);

        // Seed Products
        // Fresh Mart (Grocery)
        createProduct("prod-apples", "Apples", 120.0, "grocery", "cat-grocery", s1.getId());
        createProduct("prod-milk", "Milk", 60.0, "grocery", "cat-grocery", s1.getId());
        createProduct("prod-bread", "Bread", 40.0, "grocery", "cat-grocery", s1.getId());

        // City Pharmacy (Pharmacy)
        createProduct("prod-paracetamol", "Paracetamol", 35.0, "pharmacy", "cat-pharmacy", s2.getId());
        createProduct("prod-sanitizer", "Hand Sanitizer", 99.0, "pharmacy", "cat-pharmacy", s2.getId());
        
        System.out.println("Data Seeding Completed.");
    }

    private void createCategory(String id, String name, String slug, int order) {
        CategoryEntity c = new CategoryEntity();
        c.setId(id);
        c.setTenantId(TenantContext.getTenant());
        c.setName(name);
        c.setSlug(slug);
        c.setDisplayOrder(order);
        c.setIsActive(true);
        categoryRepository.save(c);
    }

    private void createProduct(String id, String name, double price, String categoryName, String categoryId, String storeId) {
        Product p = new Product(id, name, price, categoryName, storeId);
        p.setCategoryId(categoryId);
        p.setActive(true);
        productService.add(p);
    }
}
