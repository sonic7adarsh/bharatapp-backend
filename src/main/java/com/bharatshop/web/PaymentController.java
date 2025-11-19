package com.bharatshop.web;

import com.bharatshop.domain.PaymentVerificationResponse;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.tenant.TenantContext;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/storefront/payments")
public class PaymentController {
    private final FactoryProvider factoryProvider;
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    @Value("${razorpay.key:}")
    private String razorpayKey;

    public PaymentController(FactoryProvider factoryProvider) { this.factoryProvider = factoryProvider; }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            log.warn("Create-order unauthorized: tenant={}", TenantContext.getTenant());
            throw new UnauthorizedException("Unauthorized");
        }
        int amount = ((Number) body.getOrDefault("amount", 0)).intValue();
        String currency = (String) body.getOrDefault("currency", "INR");
        String tenant = TenantContext.getTenant();
        log.info("Create-order: userId={} tenant={} amount={} currency={}", up.getUserId(), tenant, amount, currency);
        PaymentOrder po = factoryProvider.getFactory().payments().createOrder(amount, currency);
        log.info("Create-order success: orderId={} amount={}", po.getId(), po.getAmount());
        return ResponseEntity.ok(po);
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody Map<String, Object> body) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            log.warn("Payment initiate unauthorized: tenant={}", TenantContext.getTenant());
            throw new UnauthorizedException("Unauthorized");
        }
        String tenant = TenantContext.getTenant();
        int amount = ((Number) body.getOrDefault("amount", 0)).intValue();
        String currency = (String) body.getOrDefault("currency", "INR");
        String method = (String) body.getOrDefault("method", "");
        String phone = (String) body.getOrDefault("phone", "");
        Object address = body.get("address");

        if (amount <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_INIT_FAILED", "Amount must be positive");
        }
        if (method == null || method.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_INIT_FAILED", "Payment method is required");
        }

        log.info("Initiate payment: userId={} tenant={} amount={} currency={} method={} phone_present={}", up.getUserId(), tenant, amount, currency, method, phone != null && !phone.isBlank());

        PaymentOrder po = factoryProvider.getFactory().payments().createOrder(amount, currency);
        String orderId = po.getId();

        Map<String, Object> response;
        switch (method.toLowerCase()) {
            case "upi": {
                String upiLink = buildUpiDeepLink(orderId, amount);
                response = Map.of(
                        "orderId", orderId,
                        "gateway", "native_upi",
                        "session", Map.of(
                                "upiDeepLink", upiLink
                        )
                );
                break;
            }
            case "card":
            case "netbanking":
            case "wallet": {
                // For inline Razorpay checkout, return key + provider order id
                response = Map.of(
                        "orderId", orderId,
                        "gateway", "razorpay",
                        "session", Map.of(
                                "key", razorpayKey == null ? "" : razorpayKey,
                                "razorpayOrderId", orderId
                        )
                );
                break;
            }
            default: {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_INIT_FAILED", "Unsupported payment method: " + method);
            }
        }

        return ResponseEntity.ok(response);
    }

    private String buildUpiDeepLink(String orderId, int amountPaise) {
        BigDecimal rupees = new BigDecimal(amountPaise).divide(new BigDecimal(100));
        String am = rupees.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
        String tn = URLEncoder.encode("Order " + orderId, StandardCharsets.UTF_8);
        String pa = "merchant@upi"; // demo VPA; replace when provider config is available
        String pn = URLEncoder.encode("BharatApp", StandardCharsets.UTF_8);
        return "upi://pay?pa=" + pa + "&pn=" + pn + "&am=" + am + "&tn=" + tn;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, Object> body) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            log.warn("Payment verify unauthorized: tenant={}", TenantContext.getTenant());
            throw new UnauthorizedException("Unauthorized");
        }
        String orderId = (String) body.getOrDefault("razorpay_order_id", body.get("orderId"));
        String paymentId = (String) body.getOrDefault("razorpay_payment_id", body.get("paymentId"));
        String signature = (String) body.getOrDefault("razorpay_signature", body.get("signature"));
        String tenant = TenantContext.getTenant();
        log.info("Verify payment: userId={} tenant={} orderId={} paymentId={} signaturePresent={}",
                up.getUserId(), tenant, orderId, paymentId, signature != null);
        PaymentVerificationResponse resp = factoryProvider.getFactory().payments()
                .verify(orderId, paymentId, signature);
        log.info("Verify result: status={} message={}", resp.getStatus(), resp.getMessage());
        return ResponseEntity.ok(resp);
    }
}