package com.bharatshop.web;

import com.bharatshop.service.ProductDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductDiscoveryService productDiscoveryService;

    public CategoryController(ProductDiscoveryService productDiscoveryService) {
        this.productDiscoveryService = productDiscoveryService;
    }

    @GetMapping("/global")
    public ResponseEntity<?> getGlobalCategories() {
        return ResponseEntity.ok(Map.of("categories", productDiscoveryService.getGlobalCategories()));
    }
}
