package com.bharatshop.repository;

import com.bharatshop.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {
    List<OrderItemEntity> findByOrderId(String orderId);
}