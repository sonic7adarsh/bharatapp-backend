package com.bharatshop.web;

import com.bharatshop.domain.Product;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform")
public class PlatformProductController {
    private static final Logger log = LoggerFactory.getLogger(PlatformProductController.class);
    private final FactoryProvider factoryProvider;

    public PlatformProductController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> list(@RequestParam(required = false) String category,
                                              @RequestParam(required = false) String search) {
        log.info("Platform list products: category={} search={}", category, search);
        return ResponseEntity.ok(factoryProvider.getFactory(null).products().list(category, search));
    }

    @PostMapping(value = "/products", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> add(@RequestPart(value = "name", required = false) String name,
                                 @RequestPart(value = "price", required = false) Double price,
                                 @RequestPart(value = "description", required = false) String description,
                                 @RequestPart(value = "category", required = false) String category,
                                 @RequestPart(value = "storeId", required = false) String storeId,
                                 @RequestPart(value = "image", required = false) MultipartFile image,
                                 @RequestBody(required = false) Map<String, Object> json) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Platform add product requested: name={} price={} storeId={} imagePresent={}", name, price, storeId, image != null);

        if (json != null && (name == null || price == null)) {
            name = (String) json.get("name");
            Object p = json.get("price");
            price = p == null ? null : ((Number)p).doubleValue();
            description = (String) json.get("description");
            category = (String) json.get("category");
            storeId = (String) json.get("storeId");
        }

        if (name == null || price == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "name and price are required"));
        }
        Product product = new Product(UUID.randomUUID().toString(), name, price, category, storeId);
        product.setDescription(description);
        if (image != null) product.setImage(image.getOriginalFilename());
        factoryProvider.getFactory(null).products().add(product);
        log.info("Platform add product success: id={} name={} storeId={}", product.getId(), product.getName(), product.getStoreId());
        return ResponseEntity.ok(Map.of("success", true, "product", product));
    }
}