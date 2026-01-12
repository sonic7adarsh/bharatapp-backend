package com.bharatshop.repository;

import com.bharatshop.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    List<OrderEntity> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, String userId);
    Optional<OrderEntity> findByTenantIdAndId(String tenantId, String id);
}