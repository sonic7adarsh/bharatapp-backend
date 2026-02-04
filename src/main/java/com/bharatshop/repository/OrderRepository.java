package com.bharatshop.repository;

import com.bharatshop.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    List<OrderEntity> findByStoreId(String storeId);
    Optional<OrderEntity> findByIdAndUserId(String id, String userId);
    Optional<OrderEntity> findByReference(String reference);
}