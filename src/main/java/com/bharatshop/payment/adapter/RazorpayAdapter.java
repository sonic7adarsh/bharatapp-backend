package com.bharatshop.payment.adapter;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;
import com.bharatshop.payment.PaymentGateway;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;
import java.util.UUID;

@Component
@Primary
public class RazorpayAdapter implements PaymentGateway, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RazorpayAdapter.class);

    @Value("${razorpay.key:}")
    private String keyId;

    @Value("${razorpay.secret:}")
    private String keySecret;

    private RazorpayClient client;

    @Override
    public void afterPropertiesSet() {
        try {
            if (keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank()) {
                this.client = new RazorpayClient(keyId, keySecret);
                log.info("Razorpay Client Initialized Successfully");
            } else {
                log.error("Razorpay keys missing! Payment operations will fail.");
                // We do not fallback to stub mode anymore.
            }
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay Client", e);
        }
    }

    private void ensureClient() {
        if (client == null) {
            log.error("Razorpay Client is not initialized. Keys are missing.");
            throw new RuntimeException("Payment Gateway is not configured.");
        }
    }

    @Override
    public PaymentOrder createOrder(int amount, String currency) {
        ensureClient();

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount * 100); // Razorpay expects amount in subunits (paise)
            options.put("currency", currency);
            options.put("receipt", "txn_" + UUID.randomUUID().toString().substring(0, 8));
            options.put("payment_capture", 1); // Auto capture

            log.info("Razorpay Create Order Request: {}", options.toString());
            Order order = client.orders.create(options);
            log.info("Razorpay Create Order Response: {}", order.toString());
            return new PaymentOrder(order.get("id"), amount);
        } catch (RazorpayException e) {
            log.error("Razorpay Create Order Failed", e);
            throw new RuntimeException("Gateway Error: " + e.getMessage());
        }
    }

    @Override
    public PaymentVerificationResponse verify(String orderId, String paymentId, String signature) {
        ensureClient();
        log.info("Razorpay Verify Request: orderId={} paymentId={} signature={}", orderId, paymentId, signature);
        
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            log.info("Razorpay Verify Result: isValid={}", isValid);
            if (isValid) {
                return new PaymentVerificationResponse("ok", "verified");
            } else {
                return new PaymentVerificationResponse("error", "invalid_signature");
            }
        } catch (RazorpayException e) {
            log.error("Razorpay Verification Failed", e);
            return new PaymentVerificationResponse("error", "verification_failed");
        }
    }

    @Override
    public PaymentVerificationResponse refund(String paymentId, int amount, String currency, String reason) {
        ensureClient();
        log.info("Razorpay Refund Request: paymentId={} amount={} currency={} reason={}", paymentId, amount, currency, reason);

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount * 100);
            options.put("speed", "optimum");
            options.put("notes", new JSONObject().put("reason", reason));
            
            Refund refund = client.payments.refund(paymentId, options);
            log.info("Razorpay Refund Response: {}", refund.toString());
            return new PaymentVerificationResponse("ok", refund.get("id"));
        } catch (RazorpayException e) {
            log.error("Razorpay Refund Failed", e);
            return new PaymentVerificationResponse("error", e.getMessage());
        }
    }
}