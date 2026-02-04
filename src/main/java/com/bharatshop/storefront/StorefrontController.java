package com.bharatshop.storefront;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/storefront")
public class StorefrontController {
    private static final Logger log = LoggerFactory.getLogger(StorefrontController.class);
    private final FactoryProvider factoryProvider;
    private final StoreService storeService;
    private final com.bharatshop.service.CategoryService categoryService;

    public StorefrontController(FactoryProvider factoryProvider, StoreService storeService, com.bharatshop.service.CategoryService categoryService) {
        this.factoryProvider = factoryProvider;
        this.storeService = storeService;
        this.categoryService = categoryService;
    }

    private boolean isActiveStore(Store s) {
        if (s == null) return false;
        boolean open = s.getStatus() == null || "open".equalsIgnoreCase(s.getStatus());
        boolean orderingEnabled = !Boolean.TRUE.equals(s.getOrderingDisabled());
        return open && orderingEnabled;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // convert to km
    }

    @GetMapping("/categories")
    public ResponseEntity<List<com.bharatshop.service.CategoryService.CategoryDto>> listCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @GetMapping("/stores")
    public ResponseEntity<List<StorefrontStoreDto>> listStores(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(required = false) Double lat,
                                                  @RequestParam(required = false) Double lng) {
        UserPrincipal up = UserPrincipal.current();
        log.info("Customer storefront: list stores userId={} search={} category={} lat={} lng={}", up != null ? up.getUserId() : null, search, category, lat, lng);
        
        // STRICT: Always use listNearby which enforces geo-fencing and requires location
        List<Store> all = storeService.listNearby(lat, lng, search, category);

        List<StorefrontStoreDto> result = all.stream()
                .map(s -> {
                    StorefrontStoreDto dto = new StorefrontStoreDto(s);
                    if (lat != null && lng != null && s.getLatitude() != null && s.getLongitude() != null) {
                        dto.distance = calculateDistance(lat, lng, s.getLatitude(), s.getLongitude());
                    }
                    return dto;
                })
                .sorted((a, b) -> {
                    if (a.distance == null && b.distance == null) return 0;
                    if (a.distance == null) return 1;
                    if (b.distance == null) return -1;
                    return Double.compare(a.distance, b.distance);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<StorefrontStoreDto> getStore(@PathVariable String storeId) {
        UserPrincipal up = UserPrincipal.current();
        log.info("Customer storefront: get store userId={} storeId={} ", up != null ? up.getUserId() : null, storeId);
        Store s = factoryProvider.getFactory().stores().get(storeId);
        if (s == null) return ResponseEntity.status(404).build();
        if (!isActiveStore(s)) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(new StorefrontStoreDto(s));
    }

    // DTO to hide sensitive seller info (phone, ownerId) until order is placed
    public static class StorefrontStoreDto {
        public String id;
        public String name;
        public String area;
        public String category;
        public String address;
        public Double latitude;
        public Double longitude;
        public Double distance;
        public String logo;
        public String status;
        public Boolean orderingDisabled;
        public String closedReason;
        public java.time.Instant closedUntil;

        public StorefrontStoreDto(Store s) {
            this.id = s.getId();
            this.name = s.getName();
            this.area = s.getArea();
            this.category = s.getCategory();
            this.address = s.getAddress();
            this.latitude = s.getLatitude();
            this.longitude = s.getLongitude();
            this.logo = s.getLogo();
            this.status = s.getStatus();
            this.orderingDisabled = s.getOrderingDisabled();
            this.closedReason = s.getClosedReason();
            this.closedUntil = s.getClosedUntil();
        }
    }

    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<List<Product>> productsByStore(@PathVariable String storeId,
                                                         @RequestParam(required = false) String categoryId) {
        UserPrincipal up = UserPrincipal.current();
        log.info("Customer storefront: list products by store userId={} storeId={} categoryId={}", up != null ? up.getUserId() : null, storeId, categoryId);
        Store s = factoryProvider.getFactory().stores().get(storeId);
        if (s == null) return ResponseEntity.status(404).build();
        if (!isActiveStore(s)) return ResponseEntity.status(404).build();

        // If categoryId is present, use the optimized query
        List<Product> all;
        if (categoryId != null && !categoryId.isBlank()) {
            all = factoryProvider.getFactory().products().byStoreAndCategory(storeId, categoryId);
        } else {
            all = factoryProvider.getFactory().products().byStore(storeId);
        }
        
        return ResponseEntity.ok(all);
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> listProducts(@RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String search) {
        UserPrincipal up = UserPrincipal.current();
        log.info("Customer storefront: list products userId={} category={} search={} ", up != null ? up.getUserId() : null, category, search);
        List<Product> all = factoryProvider.getFactory().products().list(category, search);
        List<Product> result = all.stream()
                .sorted(Comparator.comparing(Product::getName))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        log.info("Customer storefront: get product userId={} id={}", up != null ? up.getUserId() : null, id);
        Product p = factoryProvider.getFactory().products().get(id);
        if (p == null || !Boolean.TRUE.equals(p.getActive())) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(p);
    }
}