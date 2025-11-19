package com.bharatshop.web.seller;

import com.bharatshop.domain.Store;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.ForbiddenException;
import com.bharatshop.error.BadRequestException;
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

    public SellerStoreController(FactoryProvider factoryProvider, com.bharatshop.service.AuthService authService,
            UserRepository userRepository) {
        this.factoryProvider = factoryProvider;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    private boolean ensureAuth() {
        return UserPrincipal.current() != null;
    }

    @GetMapping("/stores")
    public ResponseEntity<Map<String, Object>> listStores(@RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, name = "limit") Integer limit,
            @RequestParam(required = false, name = "pageSize") Integer pageSize,
            @RequestParam(required = false, name = "ownerPhone") String ownerPhone,
            @RequestParam(required = false, name = "status") String status,
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        log.info(
                "Seller list stores: search={} page={} limit={} pageSize={} ownerPhone={} status={} tenant={} userId={}",
                search, page, limit, pageSize, ownerPhone, status, tenantDomain, up.getUserId());
        try {
            Integer effectiveLimit = limit != null ? limit : pageSize;
            List<Store> stores = factoryProvider.getSellerFactory(tenantDomain).stores().list(search, page,
                    effectiveLimit);
            if (stores == null)
                stores = java.util.Collections.emptyList();
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
                m.put("closedReason", s.getClosedReason());
                m.put("closedUntil", s.getClosedUntil());
                return m;
            }).toList();
            return ResponseEntity.ok(Map.of("stores", items));
        } catch (Exception ex) {
            log.error("Seller list stores failed", ex);
            throw new RuntimeException("internal_error", ex);
        }
    }

    @PostMapping(value = "/stores", consumes = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> createStore(@RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "city", required = false) String city,
            @RequestPart(value = "area", required = false) String area,
            @RequestPart(value = "category", required = false) String category,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!ensureAuth())
            throw new UnauthorizedException("Unauthorized");
        log.info("Seller create store requested: name={} city={} area={} category={} imagePresent={}", name, city, area,
                category, image != null);
        // Prefer multipart parts, fallback to JSON body if provided
        if (body != null) {
            if (name == null)
                name = (String) body.get("name");
            if (city == null)
                city = body.get("city") == null ? null : body.get("city").toString();
            if (area == null)
                area = body.get("area") == null ? null : body.get("area").toString();
            if (category == null)
                category = (String) body.get("category");
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
            log.warn("Seller create store failed: name={} city={} area={} category={} err={}", name, city, area,
                    category, ex.getMessage());
            throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "store_create_failed", "Could not create store");
        }
    }

    private boolean isSellerOrVendor(String role) {
        if (role == null)
            return false;
        String r = role.trim().toLowerCase();
        return "seller".equals(r) || "vendor".equals(r);
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        String digits = phone.replaceAll("[^0-9]", "");
        // Trim +91, 91, or leading 0
        if (digits.startsWith("91") && digits.length() > 10)
            digits = digits.substring(digits.length() - 10);
        if (digits.startsWith("0") && digits.length() > 10)
            digits = digits.substring(digits.length() - 10);
        if (digits.length() == 10)
            return digits;
        return digits.isEmpty() ? null : digits;
    }

    private String resolvePhone(String userId) {
        if (userId == null)
            return null;
        java.util.Optional<UserEntity> opt = userRepository.findById(userId);
        return opt.map(UserEntity::getPhone).orElse(null);
    }

    @PatchMapping("/stores/{storeId}")
    public ResponseEntity<?> updateStore(@PathVariable String storeId,
                                         @RequestBody Map<String, Object> changes,
                                         @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller update store: storeId={} changesKeys={} tenant={} ", storeId,
                changes != null ? changes.keySet() : java.util.Collections.emptySet(), tenantDomain);

        // Ownership enforcement (owner or admin)
        Store existing = factoryProvider.getSellerFactory(tenantDomain).stores().get(storeId);
        var principal = UserPrincipal.current();
        if (existing == null || principal == null) {
            throw new NotFoundException("Store not found");
        }
        boolean isOwner = existing.getOwnerId() != null && existing.getOwnerId().equals(principal.getUserId());
        boolean isAdmin = principal.getRole() != null && principal.getRole().equalsIgnoreCase("admin");
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Forbidden");
        }

        Map<String, Object> safe = new HashMap<>();
        if (changes == null) changes = java.util.Collections.emptyMap();
        if (changes.containsKey("name")) safe.put("name", changes.get("name"));
        if (changes.containsKey("area")) safe.put("area", changes.get("area"));
        if (changes.containsKey("city")) safe.put("city", changes.get("city"));
        if (changes.containsKey("category")) safe.put("category", changes.get("category"));
        if (changes.containsKey("status")) {
            Object stObj = changes.get("status");
            String st = stObj == null ? null : String.valueOf(stObj).trim().toLowerCase();
            if (st != null && !st.isBlank()) {
                if (!st.equals("open") && !st.equals("closed")) {
                    throw new BadRequestException("invalid_status");
                }
                safe.put("status", st);
                if (st.equals("closed")) {
                    safe.put("orderingDisabled", true);
                } else if (!changes.containsKey("orderingDisabled")) {
                    safe.put("orderingDisabled", false);
                    // Auto-clear expired closedUntil when reopening
                    if (existing.getClosedUntil() != null && java.time.Instant.now().isAfter(existing.getClosedUntil()) && !changes.containsKey("closedUntil")) {
                        safe.put("closedUntil", null);
                    }
                }
            }
        }
        if (changes.containsKey("orderingDisabled")) safe.put("orderingDisabled", changes.get("orderingDisabled"));
        if (changes.containsKey("closedReason")) safe.put("closedReason", changes.get("closedReason"));
        if (changes.containsKey("closedUntil")) {
            Object cu = changes.get("closedUntil");
            if (cu != null && String.valueOf(cu).trim().length() > 0) {
                try { java.time.Instant.parse(String.valueOf(cu)); } catch (Exception ex) {
                    throw new BadRequestException("invalid_closedUntil");
                }
                safe.put("closedUntil", String.valueOf(cu));
            } else {
                safe.put("closedUntil", null);
            }
        }
        Store updated = factoryProvider.getSellerFactory(tenantDomain).stores().updatePartial(storeId, safe);
        if (updated == null) throw new NotFoundException("Store not found");
        log.info("Seller update store success: storeId={} name={} status={} orderingDisabled={} ", updated.getId(), updated.getName(), updated.getStatus(), updated.getOrderingDisabled());
        return ResponseEntity.ok(Map.of("success", true, "store", updated));
    }

    @PatchMapping(value = "/stores/{storeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateStoreMultipart(@PathVariable String storeId,
                                                  @RequestPart(value = "name", required = false) String name,
                                                  @RequestPart(value = "area", required = false) String area,
                                                  @RequestPart(value = "city", required = false) String city,
                                                  @RequestPart(value = "category", required = false) String category,
                                                  @RequestPart(value = "status", required = false) String status,
                                                  @RequestPart(value = "orderingDisabled", required = false) String orderingDisabled,
                                                  @RequestPart(value = "closedReason", required = false) String closedReason,
                                                  @RequestPart(value = "closedUntil", required = false) String closedUntil,
                                                  @RequestPart(value = "logoFile", required = false) org.springframework.web.multipart.MultipartFile logoFile,
                                                  @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller update store (multipart): storeId={} name={} area={} category={} status={} logoPresent={} tenant={}", storeId, name, area, category, status, logoFile != null, tenantDomain);

        Store existing = factoryProvider.getSellerFactory(tenantDomain).stores().get(storeId);
        var principal = UserPrincipal.current();
        if (existing == null || principal == null) {
            throw new NotFoundException("Store not found");
        }
        boolean isOwner = existing.getOwnerId() != null && existing.getOwnerId().equals(principal.getUserId());
        boolean isAdmin = principal.getRole() != null && principal.getRole().equalsIgnoreCase("admin");
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Forbidden");
        }

        Map<String, Object> safe = new HashMap<>();
        if (name != null) safe.put("name", name);
        if (area != null) safe.put("area", area);
        if (city != null) safe.put("city", city);
        if (category != null) safe.put("category", category);
        if (status != null) {
            String st = status.trim().toLowerCase();
            if (!st.equals("open") && !st.equals("closed")) throw new BadRequestException("invalid_status");
            safe.put("status", st);
            if (st.equals("closed")) safe.put("orderingDisabled", true);
            else if (orderingDisabled == null) {
                safe.put("orderingDisabled", false);
                // Auto-clear expired closedUntil when reopening, if not explicitly provided
                if (existing.getClosedUntil() != null && java.time.Instant.now().isAfter(existing.getClosedUntil()) && closedUntil == null) {
                    safe.put("closedUntil", null);
                }
            }
        }
        if (orderingDisabled != null) {
            String a = orderingDisabled.trim().toLowerCase();
            safe.put("orderingDisabled", ("true".equals(a) || "1".equals(a)) ? Boolean.TRUE : Boolean.FALSE);
        }
        if (closedReason != null) safe.put("closedReason", closedReason);
        if (closedUntil != null) {
            String cu = closedUntil.trim();
            if (!cu.isEmpty()) {
                try { java.time.Instant.parse(cu); } catch (Exception ex) {
                    throw new BadRequestException("invalid_closedUntil");
                }
                safe.put("closedUntil", cu);
            } else {
                safe.put("closedUntil", null);
            }
        }
        if (logoFile != null && !logoFile.isEmpty()) {
            // Persist original filename; Integrations can resolve public URL via media service in future
            safe.put("logo", logoFile.getOriginalFilename());
        }

        Store updated = factoryProvider.getSellerFactory(tenantDomain).stores().updatePartial(storeId, safe);
        if (updated == null) throw new NotFoundException("Store not found");
        log.info("Seller update store (multipart) success: storeId={} name={} logo={}", updated.getId(), updated.getName(), updated.getLogo());
        return ResponseEntity.ok(Map.of("success", true, "store", updated));
    }

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<?> getStore(@PathVariable String storeId,
                                      @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        var principal = UserPrincipal.current();
        Store s = factoryProvider.getSellerFactory(tenantDomain).stores().get(storeId);
        if (s == null || principal == null || s.getOwnerId() == null || !s.getOwnerId().equals(principal.getUserId())) {
            throw new NotFoundException("Store not found");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", s.getId());
        resp.put("name", s.getName());
        resp.put("area", s.getArea());
        resp.put("category", s.getCategory());
        resp.put("ownerId", s.getOwnerId());
        resp.put("ownerPhone", s.getOwnerPhone());
        resp.put("status", s.getStatus() != null ? s.getStatus() : "open");
        resp.put("orderingDisabled", s.getOrderingDisabled() != null ? s.getOrderingDisabled() : Boolean.FALSE);
        resp.put("closedReason", s.getClosedReason());
        resp.put("closedUntil", s.getClosedUntil());
        resp.put("logo", s.getLogo());
        resp.put("updatedAt", s.getUpdatedAt());
        return ResponseEntity.ok(resp);
    }
}