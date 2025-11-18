package com.bharatshop.web;

import com.bharatshop.domain.CartItem;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    private String resolveUserId(String guestId) {
        UserPrincipal up = UserPrincipal.current();
        if (up != null) return up.getUserId();
        if (guestId != null && !guestId.isBlank()) return "guest-" + guestId;
        return "guest";
    }

    @GetMapping("/cart")
    public ResponseEntity<List<CartItem>> getCart(@RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        String userId = resolveUserId(guestId);
        log.info("Get cart: userId={}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/cart/add")
    public ResponseEntity<List<CartItem>> add(@RequestHeader(value = "X-Guest-Id", required = false) String guestId,
                                              @RequestBody CartItem item) {
        String userId = resolveUserId(guestId);
        log.info("Add cart item: userId={} itemId={} qty={} ", userId, item.getId(), item.getQuantity());
        return ResponseEntity.ok(cartService.addItem(userId, item));
    }

    @DeleteMapping("/cart/{id}")
    public ResponseEntity<List<CartItem>> remove(@RequestHeader(value = "X-Guest-Id", required = false) String guestId,
                                                 @PathVariable String id) {
        String userId = resolveUserId(guestId);
        log.info("Remove cart item: userId={} id={}", userId, id);
        return ResponseEntity.ok(cartService.removeItem(userId, id));
    }
}