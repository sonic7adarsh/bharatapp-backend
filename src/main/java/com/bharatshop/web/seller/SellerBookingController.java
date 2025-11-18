package com.bharatshop.web.seller;

import com.bharatshop.domain.Order;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
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
@RequestMapping("/api/seller")
public class SellerBookingController {
    private final FactoryProvider factoryProvider;
    public SellerBookingController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }
    private static final Logger log = LoggerFactory.getLogger(SellerBookingController.class);

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @GetMapping("/bookings")
    public ResponseEntity<?> list(@RequestParam(required = false) String storeId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String from,
                                  @RequestParam(required = false) String to,
                                  @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Seller list bookings: storeId={} status={} from={} to={} tenant={}", storeId, status, from, to, tenant);
        List<Order> dto = factoryProvider.getSellerFactory(tenant).bookings().listBookings(storeId, status, from, to);
        log.info("Seller list bookings success: count={}", dto != null ? dto.size() : 0);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String bookingId, @RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        String status = (String) body.get("status");
        if (status == null) return ResponseEntity.badRequest().body(Map.of("message", "status is required"));
        log.info("Seller update booking status: bookingId={} status={}", bookingId, status);
        Order o = factoryProvider.getSellerFactory(tenant).bookings().updateStatus(bookingId, status, (String) body.get("notes"));
        if (o == null) return ResponseEntity.status(404).body(Map.of("message", "Booking not found"));
        log.info("Seller update booking status success: bookingId={} status={}", o.getId(), o.getStatus());
        return ResponseEntity.ok(o);
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<?> get(@PathVariable String bookingId,
                                 @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Seller get booking: bookingId={} tenant={}", bookingId, tenant);
        // Fallback to list and match since bookings service may not expose get()
        List<Order> all = factoryProvider.getSellerFactory(tenant).bookings().listBookings(null, null, null, null);
        Order match = null;
        if (all != null) {
            for (Order o : all) {
                if (bookingId.equals(o.getId())) { match = o; break; }
            }
        }
        if (match == null) return ResponseEntity.status(404).body(Map.of("message", "Booking not found"));
        log.info("Seller get booking success: bookingId={} status={}", match.getId(), match.getStatus());
        return ResponseEntity.ok(match);
    }

    private Instant parseDate(String d) {
        if (d == null || d.isBlank()) return null;
        try { return LocalDate.parse(d).atStartOfDay().toInstant(java.time.ZoneOffset.UTC); }
        catch (DateTimeParseException ex) { return null; }
    }

    
}