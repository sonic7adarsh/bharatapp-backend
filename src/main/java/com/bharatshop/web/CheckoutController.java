package com.bharatshop.web;

import com.bharatshop.dto.CheckoutRequest;
import com.bharatshop.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront")
public class CheckoutController {
    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) { this.checkoutService = checkoutService; }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest req) {
        log.info("Checkout requested: items={} paymentMethod={}",
                req.getItems() != null ? req.getItems().size() : 0, req.getPaymentMethod());
        return checkoutService.checkout(req);
    }
}