package com.bharatshop.factory.ops;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;

public interface PaymentOps {
    PaymentOrder createOrder(int amount, String currency);
    PaymentVerificationResponse verify(String orderId, String paymentId, String signature);
}