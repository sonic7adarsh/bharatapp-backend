package com.bharatshop.web.seller;

import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.BadRequestException;
import com.bharatshop.factory.FactoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/seller")
public class SellerAnnouncementController {
    private static final Logger log = LoggerFactory.getLogger(SellerAnnouncementController.class);
    private final FactoryProvider factoryProvider;

    public SellerAnnouncementController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @PostMapping("/announcements")
    public ResponseEntity<?> post(@RequestBody Map<String, Object> body,
                                  @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        String storeId = (String) body.get("storeId");
        String message = (String) body.get("message");
        String activeUntil = (String) body.getOrDefault("activeUntil", null);
        if (storeId == null || message == null) throw new BadRequestException("storeId and message are required");
        log.info("Seller post announcement: storeId={} messageLen={} activeUntil={} tenant={}", storeId, message != null ? message.length() : 0, activeUntil, tenant);
        Map<String, Object> a = factoryProvider.getSellerFactory(tenant).announcements().post(storeId, message, activeUntil);
        log.info("Seller announcement posted: storeId={} idPresent={}", storeId, a != null && a.get("id") != null);
        return ResponseEntity.ok(Map.of("success", true, "announcement", a));
    }
}