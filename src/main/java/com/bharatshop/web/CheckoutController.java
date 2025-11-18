package com.bharatshop.web;

import com.bharatshop.domain.Order;
import com.bharatshop.dto.CheckoutRequest;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
public class CheckoutController {
    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final FactoryProvider factoryProvider;

    public CheckoutController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                      @Valid @RequestBody CheckoutRequest req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Checkout requested: tenant={} userId={} items={} paymentMethod={}", tenant, up.getUserId(),
                req.getItems() != null ? req.getItems().size() : 0, req.getPaymentMethod());

        // Conditional validation: items required for non-hospitality; optional for hospitality (room_booking)
        String type = req.getType() == null ? "order" : req.getType();
        if ("room_booking".equalsIgnoreCase(type)) {
            if (req.getBooking() == null) {
                return ResponseEntity.status(400).body(Map.of("message", "booking details required for hospitality"));
            }
        } else {
            if (req.getItems() == null || req.getItems().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("message", "items must not be null or empty for non-hospitality orders"));
            }
        }

        // Enforce store availability/operational state before placing order
        if (req.getStoreId() != null && !req.getStoreId().isBlank()) {
            com.bharatshop.domain.Store store = factoryProvider.getFactory(tenant).stores().get(req.getStoreId());
            if (store == null) {
                return ResponseEntity.status(400).body(java.util.Map.of("message", "Invalid storeId"));
            }
            boolean disabled = Boolean.TRUE.equals(store.getOrderingDisabled());
            boolean closed = store.getStatus() != null && store.getStatus().equalsIgnoreCase("closed");
            boolean untilClosed = store.getClosedUntil() != null && java.time.Instant.now().isBefore(store.getClosedUntil());
            if (disabled || closed || untilClosed) {
                String reason = store.getClosedReason() != null ? store.getClosedReason() : "store_unavailable";
                return ResponseEntity.status(403).body(java.util.Map.of("message", "Store not accepting orders", "reason", reason));
            }
        }

        Order order;
        if ("room_booking".equalsIgnoreCase(type)) {
            order = factoryProvider.getFactory(tenant).orders()
                    .placeBooking(up.getUserId(), req.getBooking(), req.getTotals(), req.getPaymentMethod(), req.getPaymentInfo(), req.getStoreId(), req.getNotes());
        } else {
            order = factoryProvider.getFactory(tenant).orders()
                    .placeOrder(up.getUserId(), req.getItems(), req.getTotals(), req.getPaymentMethod(), req.getPaymentInfo(), req.getType(), req.getStoreId(), req.getNotes());
        }
        order.setAddress(req.getAddress());
        order.setDeliverySlot(req.getDeliverySlot());
        order.setDeliveryInstructions(req.getDeliveryInstructions());
        order.setPromo(req.getPromo());
        order.setBooking(req.getBooking());
        log.info("Checkout success: orderId={} reference={} userId={}", order.getId(), order.getReference(), up.getUserId());
        return ResponseEntity.ok(Map.of("order", order, "reference", order.getReference()));
    }
}