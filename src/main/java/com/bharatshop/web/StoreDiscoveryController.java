package com.bharatshop.web;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.tenant.TenantContext;
import com.bharatshop.error.NotFoundException;
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

    public StoreDiscoveryController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

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

    @PostMapping("/stores")
    public ResponseEntity<?> addStore(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                      @RequestBody Store store) {
        log.info("Add store: tenant={} name={} area={} category={} ", tenant, store.getName(), store.getArea(), store.getCategory());
        if (store.getId() == null || store.getId().isBlank()) {
            store.setId(java.util.UUID.randomUUID().toString());
        }
        factoryProvider.getFactory(tenant).stores().add(store);
        log.info("Add store success: id={} name={} ", store.getId(), store.getName());
        return ResponseEntity.ok(Map.of("success", true, "store", store));
    }
}