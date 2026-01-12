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
    private final com.bharatshop.service.LogisticsService logisticsService;

    public SellerOrderController(FactoryProvider factoryProvider, com.bharatshop.service.LogisticsService logisticsService) {
        this.factoryProvider = factoryProvider;
        this.logisticsService = logisticsService;
    }

    private boolean ensureAuth() {
        com.bharatshop.security.UserPrincipal up = com.bharatshop.security.UserPrincipal.current();
        if (up == null) return false;
        String role = up.getRole();
        if (role == null) return false;
        role = role.toLowerCase();
        return role.equals("seller") || role.equals("vendor") || role.equals("admin");
    }

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

    @PostMapping("/orders/{orderId}/accept")
    public ResponseEntity<?> accept(@PathVariable String orderId,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller accept order: orderId={} tenant={}", orderId, tenant);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, "accepted", null);
        if (o == null) throw new NotFoundException("Order not found");
        return ResponseEntity.ok(Map.of("status", o.getStatus()));
    }

    @PostMapping("/orders/{orderId}/reject")
    public ResponseEntity<?> reject(@PathVariable String orderId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        String reason = body != null ? (String) body.get("reason") : null;
        log.info("Seller reject order: orderId={} tenant={} reason={}", orderId, tenant, reason);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, "rejected", reason);
        if (o == null) throw new NotFoundException("Order not found");
        return ResponseEntity.ok(Map.of("status", o.getStatus(), "reason", reason));
    }

    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<?> ship(@PathVariable String orderId,
                                  @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller ship order: orderId={} tenant={}", orderId, tenant);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, "shipped", null);
        if (o == null) throw new NotFoundException("Order not found");
        return ResponseEntity.ok(Map.of("status", o.getStatus()));
    }

    @PostMapping("/orders/{orderId}/deliver")
    public ResponseEntity<?> deliver(@PathVariable String orderId,
                                     @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        log.info("Seller deliver order: orderId={} tenant={}", orderId, tenant);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, "delivered", null);
        if (o == null) throw new NotFoundException("Order not found");
        return ResponseEntity.ok(Map.of("status", o.getStatus()));
    }

    @PatchMapping("/orders/{orderId}/items/{itemId}/status")
    public ResponseEntity<?> updateItemStatus(@PathVariable String orderId, @PathVariable String itemId,
                                              @RequestBody Map<String, Object> body,
                                              @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        String status = body.get("status") != null ? String.valueOf(body.get("status")) : null;
        if (status == null || status.isBlank()) throw new BadRequestException("status is required");
        log.info("Seller update order item status: orderId={} itemId={} status={} tenant={} ", orderId, itemId, status, tenant);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateItemStatus(orderId, itemId, status);
        if (o == null) throw new NotFoundException("Order or item not found");
        // Return minimal payload: item id and status
        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", o.getId());
        resp.put("itemId", itemId);
        resp.put("status", status);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String orderId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        String reason = body != null ? (String) body.get("reason") : null;
        log.info("Seller cancel order: orderId={} tenant={} reason={}", orderId, tenant, reason);
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, "cancelled", reason);
        if (o == null) throw new NotFoundException("Order not found");
        return ResponseEntity.ok(Map.of("status", o.getStatus(), "reason", reason));
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

    @PostMapping("/orders/{orderId}/ready")
    public ResponseEntity<?> markReady(@PathVariable String orderId,
                                       @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        if (!ensureAuth()) throw new UnauthorizedException("Unauthorized");
        Order o = factoryProvider.getSellerFactory(tenant).orders().updateStatus(orderId, "ready", null);
        if (o == null) throw new NotFoundException("Order not found");
        // Attempt rider assignment when ready
        try {
            com.bharatshop.entity.OrderDeliveryEntity d = logisticsService.assignRider(tenant != null ? tenant : com.bharatshop.tenant.TenantContext.getTenant(), orderId, o.getStoreId());
            log.info("Rider assignment upon ready: orderId={} deliveryId={} riderId={}", orderId, d != null ? d.getId() : null, d != null ? d.getRiderId() : null);
        } catch (Exception ex) {
            log.warn("Rider assignment failed: orderId={} error={}", orderId, ex.getMessage());
        }
        return ResponseEntity.ok(Map.of("status", o.getStatus()));
    }

    private Instant parseDate(String d) {
        if (d == null || d.isBlank()) return null;
        try {
            return LocalDate.parse(d).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        } catch (DateTimeParseException ex) { return null; }
    }
    
}