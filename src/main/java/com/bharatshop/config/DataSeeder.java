package com.bharatshop.config;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.service.ProductService;
import com.bharatshop.service.StoreService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    private final StoreService storeService;
    private final ProductService productService;

    public DataSeeder(StoreService storeService, ProductService productService) {
        this.storeService = storeService;
        this.productService = productService;
    }

    @Override
    public void run(String... args) {
        Store s1 = new Store(UUID.randomUUID().toString(), "Fresh Mart", "MG Road", "grocery");
        Store s2 = new Store(UUID.randomUUID().toString(), "City Pharmacy", "Brigade", "pharmacy");
        Store s3 = new Store(UUID.randomUUID().toString(), "Sunrise Hotel", "Beach", "hospitality");
        storeService.add(s1); storeService.add(s2); storeService.add(s3);

        productService.add(new Product(UUID.randomUUID().toString(), "Apples", 120.0, "fruits", s1.getId()));
        productService.add(new Product(UUID.randomUUID().toString(), "Milk", 60.0, "dairy", s1.getId()));
        productService.add(new Product(UUID.randomUUID().toString(), "Paracetamol", 35.0, "medicine", s2.getId()));
        productService.add(new Product(UUID.randomUUID().toString(), "Room Deluxe", 2500.0, "room", s3.getId()));
    }
}