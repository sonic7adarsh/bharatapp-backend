package com.bharatshop.web.seller;

import com.bharatshop.domain.Product;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.factory.FactoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
public class SellerProductController {
    private static final Logger log = LoggerFactory.getLogger(SellerProductController.class);
    private final FactoryProvider factoryProvider;

    public SellerProductController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<?> listByStore(@PathVariable String storeId,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) Boolean active,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Seller list products: storeId={} search={} category={} active={} page={} limit={} tenant={}", storeId, search, category, active, page, limit, tenant);
        List<Product> all = factoryProvider.getSellerFactory(tenant).products()
                .listByStore(storeId, search, category, active, page, limit);
        log.info("Seller list products success: count={}", all != null ? all.size() : 0);
        return ResponseEntity.ok(all);
    }

    @PostMapping("/stores/{storeId}/products")
    public ResponseEntity<?> create(@PathVariable String storeId, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        String name = (String) body.get("name");
        Object priceObj = body.get("price");
        if (name == null || priceObj == null) return ResponseEntity.badRequest().body(Map.of("message", "name and price are required"));
        double price = ((Number) priceObj).doubleValue();
        String category = (String) body.get("category");
        String currency = (String) body.getOrDefault("currency", "INR");
        String description = (String) body.get("description");
        String image = (String) body.get("image");
        String sku = (String) body.get("sku");
        Integer stock = body.get("stock") instanceof Number ? ((Number) body.get("stock")).intValue() : null;
        Boolean active = body.get("active") instanceof Boolean ? (Boolean) body.get("active") : Boolean.TRUE;
        log.info("Seller create product: storeId={} name={} price={} category={} imagePresent={} ", storeId, name, price, category, image != null);

        Product p = new Product(UUID.randomUUID().toString(), name, price, category, storeId);
        p.setCurrency(currency);
        p.setDescription(description);
        p.setImage(image);
        p.setSku(sku);
        p.setStock(stock);
        p.setActive(active);
        p = factoryProvider.getSellerFactory(tenant).products().create(storeId, p);
        log.info("Seller create product success: id={} name={} storeId={}", p.getId(), p.getName(), storeId);
        return ResponseEntity.ok(Map.of("success", true, "product", p));
    }

    @PatchMapping("/products/{productId}")
    public ResponseEntity<?> update(@PathVariable String productId, @RequestBody Map<String, Object> changes,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Seller update product: productId={} changesKeys={}", productId, changes != null ? changes.keySet() : java.util.Collections.emptySet());
        Product updated = factoryProvider.getSellerFactory(tenant).products().updatePartial(productId, changes);
        if (updated == null) return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
        log.info("Seller update product success: productId={} active={} stock={} price={}", updated.getId(), updated.getActive(), updated.getStock(), updated.getPrice());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> delete(@PathVariable String productId,
                                    @RequestParam(required = false, defaultValue = "true") boolean archive,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Seller delete product requested: productId={} archive={}", productId, archive);
        boolean ok = factoryProvider.getSellerFactory(tenant).products().delete(productId, archive);
        if (!ok) return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
        log.info("Seller delete product success: productId={} archive={}", productId, archive);
        return ResponseEntity.ok(Map.of(archive ? "archived" : "deleted", true));
    }

    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@PathVariable String productId,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Seller upload product image: productId={} filename={}", productId, file != null ? file.getOriginalFilename() : null);
        Product updated = factoryProvider.getSellerFactory(tenant).products().updateImage(productId, file.getOriginalFilename());
        if (updated == null) return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
        log.info("Seller upload image success: productId={} image={}", productId, updated.getImage());
        return ResponseEntity.ok(Map.of("success", true, "image", updated.getImage()));
    }

    @PatchMapping("/products/{productId}/inventory")
    public ResponseEntity<?> adjustInventory(@PathVariable String productId, @RequestBody Map<String, Object> body,
                                             @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        Integer stockDelta = body.get("stockDelta") instanceof Number ? ((Number) body.get("stockDelta")).intValue() : null;
        Integer stockSet = body.get("stockSet") instanceof Number ? ((Number) body.get("stockSet")).intValue() : null;
        Double price = body.get("price") instanceof Number ? ((Number) body.get("price")).doubleValue() : null;
        log.info("Seller adjust inventory: productId={} stockDelta={} stockSet={} price={}", productId, stockDelta, stockSet, price);
         Product updated = factoryProvider.getSellerFactory(tenant).products().adjustInventory(productId, stockDelta, stockSet, price);
         if (updated == null) return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
         log.info("Seller adjust inventory success: productId={} stock={} price={}", updated.getId(), updated.getStock(), updated.getPrice());
         return ResponseEntity.ok(updated);
    }
}