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
@RequestMapping("/api/seller/payouts")
public class SellerPayoutController {
    private static final Logger log = LoggerFactory.getLogger(SellerPayoutController.class);
    private final FactoryProvider factoryProvider;

    public SellerPayoutController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller list payouts: tenant={}", tenant);
        return ResponseEntity.ok(factoryProvider.getSellerFactory(tenant).payouts().list());
    }

    @PostMapping("/request")
    public ResponseEntity<?> request(@RequestBody Map<String, Object> body,
                                     @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        Number amount = (Number) body.get("amount");
        if (amount == null) throw new BadRequestException("amount is required");
        log.info("Seller payout request: amount={} tenant={}", amount, tenant);
        Map<String, Object> p = factoryProvider.getSellerFactory(tenant).payouts().request(amount.doubleValue());
        log.info("Seller payout request success: amount={} idPresent={}", amount, p != null && p.get("id") != null);
        return ResponseEntity.ok(Map.of("success", true, "payout", p));
    }

    @GetMapping("/config")
    public ResponseEntity<?> config(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller payout config fetch: tenant={}", tenant);
        return ResponseEntity.ok(factoryProvider.getSellerFactory(tenant).payouts().getConfig());
    }

    @PatchMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller payout config update: keys={} tenant={}", body != null ? body.keySet() : java.util.Collections.emptySet(), tenant);
        Map<String, Object> cfg = factoryProvider.getSellerFactory(tenant).payouts().updateConfig(body);
        log.info("Seller payout config update success: keys={}", cfg != null ? cfg.keySet() : java.util.Collections.emptySet());
        return ResponseEntity.ok(Map.of("success", true, "config", cfg));
    }
}