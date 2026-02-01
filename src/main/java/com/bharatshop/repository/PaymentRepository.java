package com.bharatshop.repository;

import com.bharatshop.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    Optional<PaymentEntity> findByGatewayOrderId(String gatewayOrderId);
    Optional<PaymentEntity> findByOrderId(String orderId);
}
