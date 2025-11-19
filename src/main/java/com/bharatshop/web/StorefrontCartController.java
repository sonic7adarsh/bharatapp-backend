package com.bharatshop.web;

import com.bharatshop.domain.CartItem;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.CartService;
import com.bharatshop.error.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront/cart")
public class StorefrontCartController {
    private static final Logger log = LoggerFactory.getLogger(StorefrontCartController.class);
    private final CartService cartService;

    public StorefrontCartController(CartService cartService) { this.cartService = cartService; }

    private String resolveUserId(String guestId) {
        UserPrincipal up = UserPrincipal.current();
        if (up != null) return up.getUserId();
        if (guestId != null && !guestId.isBlank()) return "guest-" + guestId;
        return "guest";
    }

    private Map<String, Object> toResponse(List<CartItem> items) {
        int itemsCount = 0;
        double subtotal = 0.0;
        if (items != null) {
            for (CartItem ci : items) {
                int q = ci.getQuantity();
                itemsCount += q;
                subtotal += (ci.getPrice() * q);
            }
        }
        Map<String, Object> totals = new HashMap<>();
        totals.put("itemsCount", itemsCount);
        totals.put("subtotal", subtotal);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("totals", totals);
        return resp;
    }

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                     @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        String userId = resolveUserId(guestId);
        log.info("Storefront get cart: tenant={} userId={} ", tenant, userId);
        List<CartItem> items = cartService.getCart(userId);
        return ResponseEntity.ok(toResponse(items));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                     @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
                                     @RequestBody CartItem item) {
        String userId = resolveUserId(guestId);
        log.info("Storefront add cart item: tenant={} userId={} itemId={} qty={}", tenant, userId, item.getId(), item.getQuantity());
        List<CartItem> items = cartService.addItem(userId, item);
        return ResponseEntity.ok(toResponse(items));
    }

    public static class UpdateCartItemRequest {
        public Integer quantity;
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<?> updateItem(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                        @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
                                        @PathVariable String id,
                                        @RequestBody UpdateCartItemRequest req) {
        String userId = resolveUserId(guestId);
        log.info("Storefront update cart item: tenant={} userId={} id={} quantity={} ", tenant, userId, id, req != null ? req.getQuantity() : null);
        List<CartItem> items = cartService.getCart(userId);
        if (req == null || req.getQuantity() == null) {
            throw new BadRequestException("quantity required");
        }
        int quantity = req.getQuantity();
        if (quantity <= 0) {
            items = cartService.removeItem(userId, id);
            return ResponseEntity.ok(toResponse(items));
        }
        boolean found = false;
        if (items != null) {
            for (CartItem ci : items) {
                if (id.equals(ci.getId())) {
                    ci.setQuantity(quantity);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            // If item not present, create a minimal item with provided quantity
            CartItem ci = new CartItem();
            ci.setId(id);
            ci.setName(id);
            ci.setPrice(0.0);
            ci.setQuantity(quantity);
            cartService.addItem(userId, ci);
            items = cartService.getCart(userId);
        }
        return ResponseEntity.ok(toResponse(items));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> removeItem(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                        @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
                                        @PathVariable String id) {
        String userId = resolveUserId(guestId);
        log.info("Storefront remove cart item: tenant={} userId={} id={} ", tenant, userId, id);
        List<CartItem> items = cartService.removeItem(userId, id);
        return ResponseEntity.ok(toResponse(items));
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                       @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        String userId = resolveUserId(guestId);
        log.info("Storefront clear cart: tenant={} userId={} ", tenant, userId);
        List<CartItem> items = cartService.clearCart(userId);
        return ResponseEntity.ok(toResponse(items));
    }
}