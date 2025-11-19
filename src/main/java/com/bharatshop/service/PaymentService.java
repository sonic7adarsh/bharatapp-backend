package com.bharatshop.service;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;
import com.bharatshop.payment.PaymentGateway;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public PaymentOrder createOrder(int amount, String currency) {
        return paymentGateway.createOrder(amount, currency);
    }

    public PaymentVerificationResponse verify(String orderId, String paymentId, String signature) {
        return paymentGateway.verify(orderId, paymentId, signature);
    }
}