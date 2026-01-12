package com.bharatshop.repository;

import com.bharatshop.entity.OrderDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderDeliveryRepository extends JpaRepository<OrderDeliveryEntity, String> {
    List<OrderDeliveryEntity> findByTenantIdAndOrderId(String tenantId, String orderId);
    Optional<OrderDeliveryEntity> findByTenantIdAndId(String tenantId, String id);
}