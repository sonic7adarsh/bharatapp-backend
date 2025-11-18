package com.bharatshop.web.seller;

import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.entity.UserEntity;
import com.bharatshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
public class SellerStoreController {
    private static final Logger log = LoggerFactory.getLogger(SellerStoreController.class);
    private final FactoryProvider factoryProvider;
    private final com.bharatshop.service.AuthService authService;
    private final UserRepository userRepository;

    public SellerStoreController(FactoryProvider factoryProvider, com.bharatshop.service.AuthService authService, UserRepository userRepository) {
        this.factoryProvider = factoryProvider;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @GetMapping("/stores")
    public ResponseEntity<Map<String, Object>> listStores(@RequestParam(required = false) String search,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false, name = "limit") Integer limit,
                                                 @RequestParam(required = false, name = "pageSize") Integer pageSize,
                                                 @RequestParam(required = false, name = "ownerPhone") String ownerPhone,
                                                 @RequestParam(required = false, name = "status") String status,
                                                 @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).build();
        if (!isSellerOrVendor(up.getRole())) return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        log.info("Seller list stores: search={} page={} limit={} pageSize={} ownerPhone={} status={} tenant={} userId={}", search, page, limit, pageSize, ownerPhone, status, tenantDomain, up.getUserId());
        try {
            Integer effectiveLimit = limit != null ? limit : pageSize;
            List<Store> stores = factoryProvider.getSellerFactory(tenantDomain).stores().list(search, page, effectiveLimit);
            if (stores == null) stores = java.util.Collections.emptyList();
            String normalizedFilter = normalizePhone(ownerPhone);
            if (normalizedFilter != null) {
                final String nf = normalizedFilter;
                stores = stores.stream()
                        .filter(s -> {
                            String op = normalizePhone(s.getOwnerPhone());
                            return op != null && op.equals(nf);
                        })
                        .toList();
            }
            if (status != null && !status.isBlank()) {
                String st = status.trim().toLowerCase();
                stores = stores.stream()
                        .filter(s -> s.getStatus() != null && s.getStatus().trim().toLowerCase().equals(st))
                        .toList();
            }
            List<Map<String, Object>> items = stores.stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("ownerId", s.getOwnerId());
                m.put("ownerPhone", s.getOwnerPhone());
                m.put("category", s.getCategory());
                m.put("type", s.getCategory()); // placeholder until distinct type is modeled
                m.put("status", s.getStatus() != null ? s.getStatus() : "open");
                m.put("orderingDisabled", s.getOrderingDisabled() != null ? s.getOrderingDisabled() : Boolean.FALSE);
                Map<String, Object> caps = new HashMap<>();
                caps.put("orders", Boolean.TRUE);
                boolean bookings = s.getCategory() != null && s.getCategory().trim().equalsIgnoreCase("hospitality");
                caps.put("bookings", bookings);
                m.put("capabilities", caps);
                m.put("closedUntil", s.getClosedUntil());
                return m;
            }).toList();
            return ResponseEntity.ok(Map.of("stores", items));
        } catch (Exception ex) {
            log.error("Seller list stores failed", ex);
            return ResponseEntity.status(500).body(Map.of("message", "internal_error"));
        }
    }

    @PostMapping(value = "/stores", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createStore(@RequestPart(value = "name", required = false) String name,
                                             @RequestPart(value = "city", required = false) String city,
                                             @RequestPart(value = "area", required = false) String area,
                                             @RequestPart(value = "category", required = false) String category,
                                             @RequestPart(value = "image", required = false) MultipartFile image,
                                             @RequestBody(required = false) Map<String, Object> body) {
        if (!ensureAuth()) return ResponseEntity.status(401).build();
        log.info("Seller create store requested: name={} city={} area={} category={} imagePresent={}", name, city, area, category, image != null);
        // Prefer multipart parts, fallback to JSON body if provided
        if (body != null) {
            if (name == null) name = (String) body.get("name");
            if (city == null) city = body.get("city") == null ? null : body.get("city").toString();
            if (area == null) area = body.get("area") == null ? null : body.get("area").toString();
            if (category == null) category = (String) body.get("category");
        }
        try {
            Store s = new Store();
            s.setName(name != null ? name : "");
            s.setArea(area != null ? area : city);
            s.setCategory(category);
            // set ownership
            var principal = com.bharatshop.security.UserPrincipal.current();
            if (principal != null) {
                s.setOwnerId(principal.getUserId());
                String phone = resolvePhone(principal.getUserId());
                s.setOwnerPhone(phone);
            }
            Store created = factoryProvider.getSellerFactory(null).stores().create(s);
            log.info("Seller create store success: id={} name={} ", created.getId(), created.getName());

            // Upgrade role immediately for pre-seller users without forcing re-login
            if (principal != null) {
                authService.upgradeRoleForUser(principal.getUserId(), "SELLER");
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", created.getId());
            resp.put("name", created.getName());
            resp.put("status", created.getStatus() != null ? created.getStatus() : "open");
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.warn("Seller create store failed: name={} city={} area={} category={} err={}", name, city, area, category, ex.getMessage());
            return ResponseEntity.status(422).body(Map.of(
                    "message", "Could not create store",
                    "code", "store_create_failed"
            ));
        }
    }

    private boolean isSellerOrVendor(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase();
        return "seller".equals(r) || "vendor".equals(r);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        // Trim +91, 91, or leading 0
        if (digits.startsWith("91") && digits.length() > 10) digits = digits.substring(digits.length() - 10);
        if (digits.startsWith("0") && digits.length() > 10) digits = digits.substring(digits.length() - 10);
        if (digits.length() == 10) return digits;
        return digits.isEmpty() ? null : digits;
    }

    private String resolvePhone(String userId) {
        if (userId == null) return null;
        java.util.Optional<UserEntity> opt = userRepository.findById(userId);
        return opt.map(UserEntity::getPhone).orElse(null);
    }

    @PatchMapping("/stores/{storeId}")
    public ResponseEntity<Store> updateStore(@PathVariable String storeId,
                                             @RequestBody Map<String, Object> changes) {
        if (!ensureAuth()) return ResponseEntity.status(401).build();
        log.info("Seller update store: storeId={} changesKeys={}", storeId, changes != null ? changes.keySet() : java.util.Collections.emptySet());
        Map<String,Object> safe = new HashMap<>();
        if (changes.containsKey("name")) safe.put("name", changes.get("name"));
        if (changes.containsKey("area")) safe.put("area", changes.get("area"));
        if (changes.containsKey("city")) safe.put("city", changes.get("city"));
        if (changes.containsKey("category")) safe.put("category", changes.get("category"));
        if (changes.containsKey("status")) safe.put("status", changes.get("status"));
        if (changes.containsKey("orderingDisabled")) safe.put("orderingDisabled", changes.get("orderingDisabled"));
        if (changes.containsKey("closedReason")) safe.put("closedReason", changes.get("closedReason"));
        if (changes.containsKey("closedUntil")) safe.put("closedUntil", changes.get("closedUntil"));
        Store updated = factoryProvider.getSellerFactory(null).stores().updatePartial(storeId, safe);
        if (updated == null) return ResponseEntity.notFound().build();
        log.info("Seller update store success: storeId={} name={} ", updated.getId(), updated.getName());
        return ResponseEntity.ok(updated);
    }
}