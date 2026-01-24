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
    public ResponseEntity<?> getProductsByCategory(@RequestParam String categoryId,
                                                   @RequestParam double lat,
                                                   @RequestParam double lng) {
        return ResponseEntity.ok(Map.of("products", productDiscoveryService.getProductsByCategory(categoryId, lat, lng)));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q,
                                    @RequestParam double lat,
                                    @RequestParam double lng) {
        return ResponseEntity.ok(productDiscoveryService.search(q, lat, lng));
    }
}
