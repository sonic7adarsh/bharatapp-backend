package com.bharatshop.payment;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;

public interface PaymentGateway {
    PaymentOrder createOrder(int amount, String currency);
    PaymentVerificationResponse verify(String orderId, String paymentId, String signature);
    PaymentVerificationResponse refund(String paymentId, int amount, String currency, String reason);
}
