package com.bharatshop.web;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.tenant.TenantContext;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.ErrorCode;
import com.bharatshop.policy.StoreAvailabilityPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StoreDiscoveryController {
    private static final Logger log = LoggerFactory.getLogger(StoreDiscoveryController.class);
    private final FactoryProvider factoryProvider;
    private final StoreAvailabilityPolicy storeAvailabilityPolicy;

    public StoreDiscoveryController(FactoryProvider factoryProvider, StoreAvailabilityPolicy storeAvailabilityPolicy) { 
        this.factoryProvider = factoryProvider; 
        this.storeAvailabilityPolicy = storeAvailabilityPolicy;
    }

    @GetMapping("/stores")
    public ResponseEntity<List<Store>> stores(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) String category) {
        String tenant = TenantContext.getTenant();
        log.info("Discover stores: tenant={} search={} category={}", tenant, search, category);
        return ResponseEntity.ok(factoryProvider.getFactory().stores().list(search, category));
    }

    @GetMapping("/stores/{id}")
    public ResponseEntity<?> getStore(@PathVariable String id) {
        String tenant = TenantContext.getTenant();
        log.info("Get store: tenant={} id={}", tenant, id);
        Store s = factoryProvider.getFactory().stores().get(id);
        if (s == null) throw new NotFoundException("Store not found");
        log.info("Get store success: id={} name={} ", s.getId(), s.getName());
        return ResponseEntity.ok(s);
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> productsByStore(@RequestParam String storeId) {
        String tenant = TenantContext.getTenant();
        log.info("Products by store: tenant={} storeId={}", tenant, storeId);
        return ResponseEntity.ok(factoryProvider.getFactory().products().byStore(storeId));
    }

    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<List<Product>> productsByStoreAlt(@PathVariable String storeId) {
        String tenant = TenantContext.getTenant();
        log.info("Products by store (alt): tenant={} storeId={}", tenant, storeId);
        return ResponseEntity.ok(factoryProvider.getFactory().products().byStore(storeId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> globalCategories() {
        String tenant = TenantContext.getTenant();
        log.info("Global categories requested: tenant={}", tenant);
        return ResponseEntity.ok(factoryProvider.getFactory().products().categories());
    }

    @PostMapping("/stores/{storeId}/validate")
    public ResponseEntity<?> validateStore(@PathVariable String storeId,
                                         @RequestParam(required = false) Double lat,
                                         @RequestParam(required = false) Double lng) {
        String tenant = TenantContext.getTenant();
        log.info("Validate store: tenant={} storeId={} lat={} lng={}", tenant, storeId, lat, lng);
        
        Store store = factoryProvider.getFactory(tenant).stores().get(storeId);
        if (store == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", ErrorCode.STORE_NOT_FOUND.name(),
                "message", ErrorCode.STORE_NOT_FOUND.getDefaultMessage(),
                "storeId", storeId
            ));
        }
        
        // Basic store availability check
        Map<String, Object> availabilityError = storeAvailabilityPolicy.availabilityError(store);
        if (availabilityError != null) {
            return ResponseEntity.ok(Map.of(
                "available", false,
                "reason", availabilityError
            ));
        }
        
        // Zone serviceability check if coordinates provided
        if (lat != null && lng != null) {
            Map<String, Object> zoneError = storeAvailabilityPolicy.availabilityError(store, lat, lng);
            if (zoneError != null) {
                return ResponseEntity.ok(Map.of(
                    "available", false,
                    "reason", zoneError
                ));
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "available", true,
            "message", "Store is available for ordering"
        ));
    }

    @PostMapping("/stores")
    public ResponseEntity<?> addStore(@RequestBody Store store) {
        String tenant = TenantContext.getTenant();
        log.info("Add store: tenant={} name={} area={} category={} ", tenant, store.getName(), store.getArea(), store.getCategory());
        if (store.getId() == null || store.getId().isBlank()) {
            store.setId(java.util.UUID.randomUUID().toString());
        }
        factoryProvider.getFactory(tenant).stores().add(store);
        log.info("Add store success: id={} name={} ", store.getId(), store.getName());
        return ResponseEntity.ok(Map.of("success", true, "store", store));
    }
}