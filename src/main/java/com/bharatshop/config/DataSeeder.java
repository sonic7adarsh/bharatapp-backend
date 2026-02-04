package com.bharatshop.config;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.repository.CategoryRepository;
import com.bharatshop.service.ProductService;
import com.bharatshop.service.StoreService;
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
        
        // Seed Categories
        createCategory("cat-medicine", "Medicine", "medicine", 1);
        createCategory("cat-stationary", "Stationary", "stationary", 2);
        createCategory("cat-service", "Service", "service", 3);
        createCategory("cat-grocery", "Grocery", "grocery", 4); // Keeping as backup

        // Seed Stores (with Location)
        Store s1 = new Store("store-med-plus", "Med Plus", "Connaught Place", "medicine");
        s1.setLatitude(28.6304);
        s1.setLongitude(77.2177);
        s1.setOwnerPhone("9876543210");
        s1.setAddress("Shop 12, Block A, CP, Delhi");

        Store s2 = new Store("store-student-point", "Student Point", "Laxmi Nagar", "stationary");
        s2.setLatitude(28.6300); // Nearby
        s2.setLongitude(77.2180);
        s2.setOwnerPhone("9876543211");
        s2.setAddress("Shop 4, Laxmi Nagar, Delhi");
        
        Store s3 = new Store("store-repair-hub", "Repair Hub", "Karol Bagh", "service");
        s3.setLatitude(28.6310); // Nearby
        s3.setLongitude(77.2190);
        s3.setOwnerPhone("9876543212");
        s3.setAddress("Shop 10, Karol Bagh, Delhi");

        storeService.add(s1); 
        storeService.add(s2);
        storeService.add(s3);

        // Seed Products
        // Med Plus (Medicine)
        createProduct("prod-paracetamol", "Paracetamol", 35.0, "medicine", "cat-medicine", s1.getId());
        createProduct("prod-syrup", "Cough Syrup", 120.0, "medicine", "cat-medicine", s1.getId());

        // Student Point (Stationary)
        createProduct("prod-notebook", "Classmate Notebook", 60.0, "stationary", "cat-stationary", s2.getId());
        createProduct("prod-pen", "Parker Pen", 250.0, "stationary", "cat-stationary", s2.getId());
        
        // Repair Hub (Service)
        createProduct("prod-ac-service", "AC Service", 500.0, "service", "cat-service", s3.getId());
        createProduct("prod-ro-repair", "RO Repair", 300.0, "service", "cat-service", s3.getId());
        
        System.out.println("Data Seeding Completed.");
    }

    private void createCategory(String id, String name, String slug, int order) {
        CategoryEntity c = new CategoryEntity();
        c.setId(id);
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
