package com.bharatshop.web;

import com.bharatshop.domain.Order;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
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
    public ResponseEntity<?> orders(@RequestHeader(value = "X-Tenant-Domain") String tenant) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("List orders: tenant={} userId={}", tenant, up.getUserId());
        List<Order> orders = factoryProvider.getFactory(tenant).orders().listOrders(up.getUserId());
        log.info("Orders fetched: count={}", orders != null ? orders.size() : 0);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> orderDetail(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                         @PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Order detail: tenant={} userId={} id={}", tenant, up.getUserId(), id);
        List<Order> orders = factoryProvider.getFactory(tenant).orders().listOrders(up.getUserId());
        Order match = null;
        if (orders != null) {
            for (Order o : orders) {
                if (id.equals(o.getId())) { match = o; break; }
            }
        }
        if (match == null) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        log.info("Order detail success: id={} status={}", match.getId(), match.getStatus());
        return ResponseEntity.ok(match);
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> bookings(@RequestHeader(value = "X-Tenant-Domain") String tenant) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("List bookings: tenant={} userId={}", tenant, up.getUserId());
        return ResponseEntity.ok(factoryProvider.getFactory(tenant).orders().listBookings(up.getUserId()));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<?> bookingDetail(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                           @PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Booking detail: tenant={} userId={} id={}", tenant, up.getUserId(), id);
        List<Order> bookings = factoryProvider.getFactory(tenant).orders().listBookings(up.getUserId());
        Order match = null;
        if (bookings != null) {
            for (Order o : bookings) {
                if (id.equals(o.getId())) { match = o; break; }
            }
        }
        if (match == null) return ResponseEntity.status(404).body(Map.of("message", "Booking not found"));
        log.info("Booking detail success: id={} status={}", match.getId(), match.getStatus());
        return ResponseEntity.ok(match);
    }
}