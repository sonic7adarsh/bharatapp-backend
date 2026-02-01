package com.bharatshop.repository;

import com.bharatshop.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    List<TransactionEntity> findByOrderId(String orderId);
    List<TransactionEntity> findByPaymentId(String paymentId);
}
