package com.bharatshop.web;

import com.bharatshop.domain.Product;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.factory.StorefrontFactory;
import com.bharatshop.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
public class StorefrontProductController {
    private static final Logger log = LoggerFactory.getLogger(StorefrontProductController.class);
    private final FactoryProvider factoryProvider;

    public StorefrontProductController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

    private StorefrontFactory factory(String tenant) { return factoryProvider.getFactory(tenant); }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> list(@RequestParam(required = false) String category,
                                              @RequestParam(required = false) String search) {
        String tenant = TenantContext.getTenant();
        log.info("Storefront list products: tenant={} category={} search={} ", tenant, category, search);
        return ResponseEntity.ok(factoryProvider.getFactory().products().list(category, search));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        String tenant = TenantContext.getTenant();
        log.info("Storefront get product: tenant={} id={}", tenant, id);
        Product p = factoryProvider.getFactory().products().get(id);
        if (p == null) throw new NotFoundException("Product not found");
        log.info("Storefront get product success: id={} name={}", p.getId(), p.getName());
        return ResponseEntity.ok(p);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        String tenant = TenantContext.getTenant();
        log.info("Storefront list categories: tenant={}", tenant);
        return ResponseEntity.ok(factoryProvider.getFactory().products().categories());
    }
}