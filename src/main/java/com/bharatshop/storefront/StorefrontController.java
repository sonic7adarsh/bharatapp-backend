package com.bharatshop.storefront;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
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

    public StorefrontController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    private boolean isActiveStore(Store s) {
        if (s == null) return false;
        boolean open = s.getStatus() == null || "open".equalsIgnoreCase(s.getStatus());
        boolean orderingEnabled = !Boolean.TRUE.equals(s.getOrderingDisabled());
        return open && orderingEnabled;
    }

    @GetMapping("/stores")
    public ResponseEntity<List<Store>> listStores(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String category) {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: list stores tenant={} userId={} search={} category={}", tenant, up != null ? up.getUserId() : null, search, category);
        List<Store> all = factoryProvider.getFactory().stores().list(search, category);
        for (Store s : all) {
            log.error("[PROOF][STORE_LIST] store.id={} tenant={} source=DB", s != null ? s.getId() : null, tenant);
        }
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
        log.error("[PROOF][STORE_API] requested storeId={}", storeId);
        Store s = factoryProvider.getFactory().stores().get(storeId);
        log.error("[PROOF][STORE_API] store.from.db id={}", s != null ? s.getId() : null);
        if (s == null) return ResponseEntity.status(404).build();
        if (!isActiveStore(s)) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(s);
    }

    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<List<Product>> productsByStore(@PathVariable String storeId) {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: list products by store tenant={} userId={} storeId={} ", tenant, up != null ? up.getUserId() : null, storeId);
        log.error("[PROOF][PRODUCT_API] requested storeId={}", storeId);
        Store s = factoryProvider.getFactory().stores().get(storeId);
        if (s == null) return ResponseEntity.status(404).build();
        if (!isActiveStore(s)) return ResponseEntity.status(404).build();

        List<Product> all = factoryProvider.getFactory().products().byStore(storeId);
        for (Product p : all) {
            log.error("[PROOF][PRODUCT_DB] productId={} storeId={} tenant={}",
                    p != null ? p.getId() : null,
                    p != null ? p.getStoreId() : null,
                    tenant);
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

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        UserPrincipal up = UserPrincipal.current();
        String tenant = TenantContext.getTenant();
        log.info("Customer storefront: list categories tenant={} userId={}", tenant, up != null ? up.getUserId() : null);
        List<Product> all = factoryProvider.getFactory().products().list(null, null);
        List<String> cats = all.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(cats);
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