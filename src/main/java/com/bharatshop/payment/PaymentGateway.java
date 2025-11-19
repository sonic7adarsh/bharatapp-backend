package com.bharatshop.payment;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;

public interface PaymentGateway {
    PaymentOrder createOrder(int amount, String currency);
    PaymentVerificationResponse verify(String orderId, String paymentId, String signature);
}