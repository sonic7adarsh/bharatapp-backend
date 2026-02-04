package com.bharatshop.service;

import com.bharatshop.domain.PaymentOrder;
import com.bharatshop.domain.PaymentVerificationResponse;
import com.bharatshop.entity.PaymentEntity;
import com.bharatshop.entity.TransactionEntity;
import com.bharatshop.payment.PaymentGateway;
import com.bharatshop.repository.PaymentRepository;
import com.bharatshop.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    public PaymentService(PaymentGateway paymentGateway,
                          PaymentRepository paymentRepository,
                          TransactionRepository transactionRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public PaymentOrder createOrder(int amount, String currency) {
        // 1. Call Gateway
        PaymentOrder order = paymentGateway.createOrder(amount, currency);
        
        // 2. Persist Intent (ACID: Ensure we track what we asked the gateway)
        PaymentEntity entity = new PaymentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setGateway("RAZORPAY");
        entity.setGatewayOrderId(order.getId());
        entity.setAmount((double) amount);
        entity.setCurrency(currency);
        entity.setStatus("CREATED");
        
        paymentRepository.save(entity);
        log.info("Payment Intent Created: {} (Gateway Order: {})", entity.getId(), order.getId());

        return order;
    }

    @Transactional
    public PaymentVerificationResponse verify(String gatewayOrderId, String paymentId, String signature) {
        // 1. Verify with Gateway
        PaymentVerificationResponse response = paymentGateway.verify(gatewayOrderId, paymentId, signature);

        // 2. Find Payment Entity
        Optional<PaymentEntity> paymentOpt = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (paymentOpt.isEmpty()) {
            log.error("Payment Entity not found for Gateway Order: {}", gatewayOrderId);
            return response; 
        }

        PaymentEntity payment = paymentOpt.get();

        // 3. Update Entity & Log Transaction
        if ("ok".equals(response.getStatus())) {
            payment.setStatus("CAPTURED");
            payment.setGatewayPaymentId(paymentId);
            payment.setGatewaySignature(signature);
            paymentRepository.save(payment);

            // Record Transaction
            TransactionEntity txn = new TransactionEntity();
            txn.setId(UUID.randomUUID().toString());
            // tenantId removed
            txn.setPaymentId(payment.getId());
            txn.setType("PAYMENT");
            txn.setStatus("SUCCESS");
            txn.setAmount(payment.getAmount());
            txn.setCurrency(payment.getCurrency());
            txn.setReferenceId(paymentId);
            txn.setDescription("Payment Captured via Razorpay");
            transactionRepository.save(txn);
            
            log.info("Payment Captured & Transaction Recorded: {}", payment.getId());
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            
            TransactionEntity txn = new TransactionEntity();
            txn.setId(UUID.randomUUID().toString());
            txn.setPaymentId(payment.getId());
            txn.setType("PAYMENT");
            txn.setStatus("FAILED");
            txn.setAmount(payment.getAmount());
            txn.setCurrency(payment.getCurrency());
            txn.setDescription("Payment Verification Failed");
            transactionRepository.save(txn);
        }

        return response;
    }

    @Transactional
    public void linkOrder(String gatewayOrderId, String shopOrderId) {
        Optional<PaymentEntity> paymentOpt = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (paymentOpt.isPresent()) {
            PaymentEntity payment = paymentOpt.get();
            payment.setOrderId(shopOrderId);
            paymentRepository.save(payment);
            log.info("Linked Payment {} to Shop Order {}", payment.getId(), shopOrderId);
        } else {
            log.warn("Payment not found for Gateway Order: {} while linking to Shop Order: {}", gatewayOrderId, shopOrderId);
        }
    }

    @Transactional
    public void processRefundForShopOrder(String shopOrderId, String reason) {
        Optional<PaymentEntity> paymentOpt = paymentRepository.findByOrderId(shopOrderId);
        if (paymentOpt.isPresent()) {
            PaymentEntity payment = paymentOpt.get();
            processRefundForPayment(payment, reason);
        } else {
            log.info("No payment found for Shop Order: {}. Skipping refund.", shopOrderId);
        }
    }

    @Transactional
    public void processRefundForOrder(String gatewayOrderId, String reason) {
        // Find Payment by Gateway Order ID
        Optional<PaymentEntity> paymentOpt = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        
        if (paymentOpt.isPresent()) {
             processRefundForPayment(paymentOpt.get(), reason);
        } else {
            log.warn("No payment found for Gateway Order: {}", gatewayOrderId);
        }
    }

    private void processRefundForPayment(PaymentEntity payment, String reason) {
         if ("CAPTURED".equals(payment.getStatus())) {
             log.info("Initiating Refund for Payment: {}", payment.getId());
             
             PaymentVerificationResponse resp = paymentGateway.refund(payment.getGatewayPaymentId(), payment.getAmount().intValue(), payment.getCurrency(), reason);
             
             TransactionEntity txn = new TransactionEntity();
             txn.setId(UUID.randomUUID().toString());
             txn.setPaymentId(payment.getId());
             txn.setType("REFUND");
             txn.setAmount(payment.getAmount());
             txn.setCurrency(payment.getCurrency());
             txn.setDescription("Refund: " + reason);
             
             if ("ok".equals(resp.getStatus())) {
                 payment.setStatus("REFUNDED");
                 txn.setStatus("SUCCESS");
                 txn.setReferenceId(resp.getMessage()); // Refund ID
                 log.info("Refund Successful. ID: {}", resp.getMessage());
             } else {
                 txn.setStatus("FAILED");
                 txn.setDescription("Refund Failed: " + resp.getMessage());
                 log.error("Refund Failed: {}", resp.getMessage());
             }
             
             paymentRepository.save(payment);
             transactionRepository.save(txn);
         } else {
             log.warn("Payment not in CAPTURED state. Cannot refund. Status: {}", payment.getStatus());
         }
    }
}
