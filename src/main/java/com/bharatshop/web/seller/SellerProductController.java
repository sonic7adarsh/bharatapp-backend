package com.bharatshop.web.seller;

import com.bharatshop.domain.Product;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {
    private static final Logger log = LoggerFactory.getLogger(SellerProductController.class);
    private final FactoryProvider factoryProvider;

    public SellerProductController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    // RBAC via @PreAuthorize; avoid manual checks

    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<?> listByStore(@PathVariable String storeId,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) Boolean active,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer limit) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        log.info("Seller list products: storeId={} search={} category={} active={} page={} limit={} tenant={}", storeId, search, category, active, page, limit, tenant);
        List<Product> all = factoryProvider.getSellerFactory(tenant).products()
                .listByStore(storeId, search, category, active, page, limit);
        log.info("Seller list products success: count={}", all != null ? all.size() : 0);
        return ResponseEntity.ok(all);
    }

    @PostMapping(value = "/stores/{storeId}/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createJson(@PathVariable String storeId,
                                        @RequestBody Map<String, Object> body) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();

        String name = (String) body.get("name");
        Double price = null;
        Object priceObj = body.get("price");
        if (priceObj != null) {
            try { price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : Double.parseDouble(String.valueOf(priceObj)); } catch (Exception ignored) {}
        }
        String description = (String) body.get("description");
        String category = (String) body.get("category");
        String currency = body.get("currency") == null ? null : String.valueOf(body.get("currency"));
        String sku = (String) body.get("sku");
        Integer stock = null;
        Object stockObj = body.get("stock");
        if (stockObj != null) {
            try { stock = stockObj instanceof Number ? ((Number) stockObj).intValue() : Integer.parseInt(String.valueOf(stockObj)); } catch (Exception ignored) {}
        }
        Boolean active = null;
        Object activeObj = body.get("active");
        if (activeObj != null) {
            String a = String.valueOf(activeObj).trim().toLowerCase();
            if ("true".equals(a) || "1".equals(a)) active = Boolean.TRUE;
            else if ("false".equals(a) || "0".equals(a)) active = Boolean.FALSE;
        }
        if (active == null) active = Boolean.TRUE;
        if (currency == null || currency.isBlank()) currency = "INR";

        if (name == null || price == null) {
            throw new com.bharatshop.error.BadRequestException("name and price are required");
        }

        String image = body.get("image") != null ? String.valueOf(body.get("image")) : null;

        log.info("Seller create product (JSON): storeId={} name={} price={} category={} imagePresent={}", storeId, name, price, category, image != null);
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

    @PostMapping(value = "/stores/{storeId}/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipart(@PathVariable String storeId,
                                            @RequestPart(value = "name", required = false) String name,
                                            @RequestPart(value = "price", required = false) String priceStr,
                                            @RequestPart(value = "description", required = false) String description,
                                            @RequestPart(value = "category", required = false) String category,
                                            @RequestPart(value = "currency", required = false) String currency,
                                            @RequestPart(value = "sku", required = false) String sku,
                                            @RequestPart(value = "stock", required = false) String stockStr,
                                            @RequestPart(value = "active", required = false) String activeStr,
                                            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();

        Double price = null;
        try { if (priceStr != null && !priceStr.isBlank()) price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
        Integer stock = null;
        try { if (stockStr != null && !stockStr.isBlank()) stock = Integer.parseInt(stockStr); } catch (Exception ignored) {}
        Boolean active = null;
        if (activeStr != null && !activeStr.isBlank()) {
            String a = activeStr.trim().toLowerCase();
            if ("true".equals(a) || "1".equals(a)) active = Boolean.TRUE;
            else if ("false".equals(a) || "0".equals(a)) active = Boolean.FALSE;
        }
        if (active == null) active = Boolean.TRUE; // default
        if (currency == null || currency.isBlank()) currency = "INR";

        if (name == null || price == null) {
            throw new com.bharatshop.error.BadRequestException("name and price are required");
        }

        String image = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            image = imageFile.getOriginalFilename();
        }

        log.info("Seller create product (multipart): storeId={} name={} price={} category={} imagePresent={}", storeId, name, price, category, image != null);

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
    public ResponseEntity<?> update(@PathVariable String productId, @RequestBody Map<String, Object> changes) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        log.info("Seller update product: productId={} changesKeys={}", productId, changes != null ? changes.keySet() : java.util.Collections.emptySet());
        Product updated = factoryProvider.getSellerFactory(tenant).products().updatePartial(productId, changes);
        if (updated == null) throw new NotFoundException("Product not found");
        log.info("Seller update product success: productId={} active={} stock={} price={}", updated.getId(), updated.getActive(), updated.getStock(), updated.getPrice());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> delete(@PathVariable String productId,
                                    @RequestParam(required = false, defaultValue = "true") boolean archive) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        log.info("Seller delete product requested: productId={} archive={}", productId, archive);
        boolean ok = factoryProvider.getSellerFactory(tenant).products().delete(productId, archive);
        if (!ok) throw new NotFoundException("Product not found");
        log.info("Seller delete product success: productId={} archive={}", productId, archive);
        return ResponseEntity.ok(Map.of(archive ? "archived" : "deleted", true));
    }

    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@PathVariable String productId,
                                         @RequestPart("file") MultipartFile file) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        log.info("Seller upload product image: productId={} filename={}", productId, file != null ? file.getOriginalFilename() : null);
        Product updated = factoryProvider.getSellerFactory(tenant).products().updateImage(productId, file.getOriginalFilename());
        if (updated == null) throw new NotFoundException("Product not found");
        log.info("Seller upload image success: productId={} image={}", productId, updated.getImage());
        return ResponseEntity.ok(Map.of("success", true, "image", updated.getImage()));
    }

    @PatchMapping("/products/{productId}/inventory")
    public ResponseEntity<?> adjustInventory(@PathVariable String productId, @RequestBody Map<String, Object> body) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        Integer stockDelta = body.get("stockDelta") instanceof Number ? ((Number) body.get("stockDelta")).intValue() : null;
        Integer stockSet = body.get("stockSet") instanceof Number ? ((Number) body.get("stockSet")).intValue() : null;
        Double price = body.get("price") instanceof Number ? ((Number) body.get("price")).doubleValue() : null;
        log.info("Seller adjust inventory: productId={} stockDelta={} stockSet={} price={}", productId, stockDelta, stockSet, price);
         Product updated = factoryProvider.getSellerFactory(tenant).products().adjustInventory(productId, stockDelta, stockSet, price);
         if (updated == null) throw new NotFoundException("Product not found");
         log.info("Seller adjust inventory success: productId={} stock={} price={}", updated.getId(), updated.getStock(), updated.getPrice());
         return ResponseEntity.ok(updated);
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) Boolean active,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer limit) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        List<com.bharatshop.domain.Store> stores = factoryProvider.getSellerFactory(tenant).stores().list(null, null, null);
        if (stores.isEmpty()) return ResponseEntity.ok(List.of());
        String storeId = stores.get(0).getId();
        return listByStore(storeId, search, category, active, page, limit);
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        List<com.bharatshop.domain.Store> stores = factoryProvider.getSellerFactory(tenant).stores().list(null, null, null);
        if (stores.isEmpty()) throw new com.bharatshop.error.BadRequestException("No store found. Create a store first.");
        String storeId = stores.get(0).getId();
        return createJson(storeId, body);
    }
}