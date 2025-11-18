package com.bharatshop.web;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/storefront/payments")
public class PaymentController {
    private final FactoryProvider factoryProvider;
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                         @RequestBody Map<String, Object> body) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            log.warn("Create-order unauthorized: tenant={}", tenant);
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        int amount = ((Number) body.getOrDefault("amount", 0)).intValue();
        String currency = (String) body.getOrDefault("currency", "INR");
        log.info("Create-order: userId={} tenant={} amount={} currency={}", up.getUserId(), tenant, amount, currency);
        PaymentOrder po = factoryProvider.getFactory(tenant).payments().createOrder(amount, currency);
        log.info("Create-order success: orderId={} amount={}", po.getId(), po.getAmount());
        return ResponseEntity.ok(po);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestHeader(value = "X-Tenant-Domain") String tenant,
                                    @RequestBody Map<String, Object> body) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            log.warn("Payment verify unauthorized: tenant={}", tenant);
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        String orderId = (String) body.getOrDefault("razorpay_order_id", body.get("orderId"));
        String paymentId = (String) body.getOrDefault("razorpay_payment_id", body.get("paymentId"));
        String signature = (String) body.getOrDefault("razorpay_signature", body.get("signature"));
        log.info("Verify payment: userId={} tenant={} orderId={} paymentId={} signaturePresent={}",
                up.getUserId(), tenant, orderId, paymentId, signature != null);
        PaymentVerificationResponse resp = factoryProvider.getFactory(tenant).payments()
                .verify(orderId, paymentId, signature);
        log.info("Verify result: status={} message={}", resp.getStatus(), resp.getMessage());
        return ResponseEntity.ok(resp);
    }
}