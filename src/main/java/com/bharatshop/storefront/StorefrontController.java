package com.bharatshop.storefront;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.StoreService;
import com.bharatshop.tenant.TenantContext;
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

    @GetMapping("/categories")
    public ResponseEntity<List<com.bharatshop.service.CategoryService.CategoryDto>> listCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @GetMapping("/stores")
    public ResponseEntity<List<Store>> listStores(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(required = false) Double lat,
                                                  @RequestParam(required = false) Double lng) {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: list stores tenant={} userId={} search={} category={} lat={} lng={}", tenant, up != null ? up.getUserId() : null, search, category, lat, lng);
        
        // STRICT: Always use listNearby which enforces geo-fencing and requires location
        List<Store> all = storeService.listNearby(lat, lng, search, category);

        List<Store> result = all.stream()
                .sorted(Comparator.comparing(Store::getName))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<Store> getStore(@PathVariable String storeId) {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: get store tenant={} userId={} storeId={} ", tenant, up != null ? up.getUserId() : null, storeId);
        Store s = factoryProvider.getFactory().stores().get(storeId);
        if (s == null) return ResponseEntity.status(404).build();
        if (!isActiveStore(s)) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(s);
    }

    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<List<Product>> productsByStore(@PathVariable String storeId,
                                                         @RequestParam(required = false) String categoryId) {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: list products by store tenant={} userId={} storeId={} categoryId={}", tenant, up != null ? up.getUserId() : null, storeId, categoryId);
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
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: list products tenant={} userId={} category={} search={} ", tenant, up != null ? up.getUserId() : null, category, search);
        List<Product> all = factoryProvider.getFactory().products().list(category, search);
        List<Product> result = all.stream()
                .sorted(Comparator.comparing(Product::getName))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: get product tenant={} userId={} id={}", tenant, up != null ? up.getUserId() : null, id);
        Product p = factoryProvider.getFactory().products().get(id);
        if (p == null || !Boolean.TRUE.equals(p.getActive())) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(p);
    }
}