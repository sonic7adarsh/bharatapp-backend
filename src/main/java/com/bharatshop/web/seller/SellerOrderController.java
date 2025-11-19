package com.bharatshop.web.seller;

import com.bharatshop.domain.CartItem;
import com.bharatshop.domain.Order;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bharatshop.error.BadRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
public class SellerOrderController {
    private static final Logger log = LoggerFactory.getLogger(SellerOrderController.class);
    private final FactoryProvider factoryProvider;

    public SellerOrderController(FactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    private boolean ensureAuth() { return UserPrincipal.current() != null; }

    @GetMapping("/orders")
    public ResponseEntity<?> list(@RequestParam(required = false) String storeId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String from,
                                  @RequestParam(required = false) String to,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer limit,
                                  @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller list orders: storeId={} status={} from={} to={} page={} limit={} tenant={}", storeId, status, from, to, page, limit, tenant);
        List<Order> dto = factoryProvider.getSellerFactory(tenant).orders().list(storeId, status, from, to, page, limit);
        log.info("Seller list orders success: count={}", dto != null ? dto.size() : 0);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> get(@PathVariable String orderId,
                                 @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller get order: orderId={} tenant={}", orderId, tenant);
        Order o = factoryProvider.getSellerFactory(tenant).orders().get(orderId);
        if (o == null) throw new NotFoundException("Order not found");
        log.info("Seller get order success: orderId={} status={} total={}", o.getId(), o.getStatus(), o.getTotal());
        return ResponseEntity.ok(o);
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String orderId, @RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        String status = (String) body.get("status");
        if (status == null) throw new BadRequestException("status is required");
        String notes = (String) body.get("notes");
        log.info("Seller update order status: orderId={} status={} notesPresent={}", orderId, status, notes != null);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, status, notes);
        if (o == null) throw new NotFoundException("Order not found");
        log.info("Seller update order status success: orderId={} status={}", o.getId(), o.getStatus());
        return ResponseEntity.ok(o);
    }

    @PostMapping("/orders/{orderId}/refunds")
    public ResponseEntity<?> refund(@PathVariable String orderId, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        Number amount = (Number) body.get("amount");
        String reason = (String) body.get("reason");
        if (amount == null) throw new BadRequestException("amount is required");
        log.info("Seller refund order: orderId={} amount={} reasonPresent={}", orderId, amount, reason != null);
        Map<String, Object> resp = factoryProvider.getSellerFactory(tenant).orders().refund(orderId, amount.doubleValue(), reason);
        if (resp == null) throw new NotFoundException("Order not found");
        log.info("Seller refund created: orderId={} amount={} ", orderId, amount);
        return ResponseEntity.ok(resp);
    }

    private Instant parseDate(String d) {
        if (d == null || d.isBlank()) return null;
        try {
            return LocalDate.parse(d).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        } catch (DateTimeParseException ex) { return null; }
    }
    
}