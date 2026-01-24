package com.bharatshop.web;

import com.bharatshop.service.ProductDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final ProductDiscoveryService productDiscoveryService;

    public SearchController(ProductDiscoveryService productDiscoveryService) {
        this.productDiscoveryService = productDiscoveryService;
    }

    @GetMapping("/products/by-category")
    public ResponseEntity<?> getProductsByCategory(
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng
    ) {
        // Lat/Lng mandatory per requirements? "Resolve nearby stores using lat/lng"
        // Prompt says "Resolve nearby stores using lat/lng". If missing, maybe return empty or error.
        // Prompt says "Scope search only to nearby stores (lat/lng mandatory)" for search.
        // For by-category: "Resolve nearby stores using lat/lng".
        // I will make them optional in param but handle logic if null.
        // If null, service returns empty list (as implemented).
        
        return ResponseEntity.ok(Map.of(
                "products", productDiscoveryService.getProductsByCategory(categoryId, lat, lng)
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("q") String query,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng
    ) {
        return ResponseEntity.ok(productDiscoveryService.search(query, lat, lng));
    }
}
