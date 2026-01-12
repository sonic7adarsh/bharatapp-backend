package com.bharatshop.web;

import com.bharatshop.domain.Order;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.tenant.TenantContext;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
public class OrdersController {
    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);
    private final FactoryProvider factoryProvider;

    public OrdersController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

    @GetMapping("/orders")
    public ResponseEntity<?> orders() {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        String tenant = TenantContext.getTenant();
        log.info("List orders: tenant={} userId={}", tenant, up.getUserId());
        List<Order> orders = factoryProvider.getFactory().orders().listOrders(up.getUserId());
        log.info("Orders fetched: count={}", orders != null ? orders.size() : 0);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> orderDetail(@PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        String tenant = TenantContext.getTenant();
        log.info("Order detail: tenant={} userId={} id={}", tenant, up.getUserId(), id);
        
        // Use tenant-scoped order lookup if tenant is available
        if (tenant != null) {
            // Try to find order directly with tenant scoping
            // This would require a new method in OrderService - for now use existing approach
            List<Order> orders = factoryProvider.getFactory().orders().listOrders(up.getUserId());
            Order match = null;
            if (orders != null) {
                for (Order o : orders) {
                    if (id.equals(o.getId())) { match = o; break; }
                }
            }
            if (match == null) throw new NotFoundException("Order not found");
            log.info("Order detail success: id={} status={}", match.getId(), match.getStatus());
            return ResponseEntity.ok(match);
        } else {
            // Fallback to existing behavior
            List<Order> orders = factoryProvider.getFactory().orders().listOrders(up.getUserId());
            Order match = null;
            if (orders != null) {
                for (Order o : orders) {
                    if (id.equals(o.getId())) { match = o; break; }
                }
            }
            if (match == null) throw new NotFoundException("Order not found");
            log.info("Order detail success: id={} status={}", match.getId(), match.getStatus());
            return ResponseEntity.ok(match);
        }
    }



    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId,
                                         @RequestBody(required = false) Map<String, Object> body) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        String tenant = TenantContext.getTenant();
        String reason = body != null ? (String) body.getOrDefault("reason", null) : null;
        log.info("Buyer cancel order: tenant={} userId={} orderId={} reason={}", tenant, up.getUserId(), orderId, reason);
        Order o = factoryProvider.getFactory().orders().cancelOrder(up.getUserId(), orderId, reason);
        if (o == null) throw new NotFoundException("Order not found");
        return ResponseEntity.ok(Map.of("status", o.getStatus(), "reason", o.getCancellationReason()));
    }
}