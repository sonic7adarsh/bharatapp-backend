package com.bharatshop.service;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
public class PaymentService {
    @Value("${razorpay.secret:}")
    private String razorpaySecret;

    public PaymentOrder createOrder(int amount, String currency) {
        return new PaymentOrder(UUID.randomUUID().toString(), amount);
    }

    public PaymentVerificationResponse verify(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) {
            return new PaymentVerificationResponse("error", "missing_fields");
        }
        if (razorpaySecret == null || razorpaySecret.isBlank()) {
            // In non-prod or missing secret, treat as unverifiable
            return new PaymentVerificationResponse("error", "secret_not_configured");
        }
        try {
            String payload = orderId + '|' + paymentId;
            String expected = hmacSha256Base64(payload, razorpaySecret);
            if (constantTimeEquals(expected, signature)) {
                return new PaymentVerificationResponse("ok", "verified");
            }
            return new PaymentVerificationResponse("error", "invalid_signature");
        } catch (Exception e) {
            return new PaymentVerificationResponse("error", "verification_failed");
        }
    }

    private static String hmacSha256Base64(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}