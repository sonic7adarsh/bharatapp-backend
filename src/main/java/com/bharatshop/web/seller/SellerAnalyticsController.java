package com.bharatshop.web.seller;

import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller/analytics")
public class SellerAnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(SellerAnalyticsController.class);
    private final FactoryProvider factoryProvider;

    public SellerAnalyticsController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(@RequestParam(required = false) String storeId,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller analytics overview: storeId={} from={} to={} tenant={}", storeId, from, to, tenant);
        Map<String, Object> resp = factoryProvider.getSellerFactory(tenant).analytics().overview(storeId, from, to);
        log.info("Seller analytics overview success: keys={}", resp != null ? resp.keySet() : java.util.Collections.emptySet());
        return ResponseEntity.ok(resp);
    }

    private Instant parseDate(String d) {
        if (d == null || d.isBlank()) return null;
        try { return LocalDate.parse(d).atStartOfDay().toInstant(java.time.ZoneOffset.UTC); }
        catch (DateTimeParseException ex) { return null; }
    }
}