package com.bharatshop.repository;

import com.bharatshop.entity.RiderEarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiderEarningRepository extends JpaRepository<RiderEarningEntity, String> {
    List<RiderEarningEntity> findByTenantIdAndRiderId(String tenantId, String riderId);
    java.util.Optional<RiderEarningEntity> findFirstByTenantIdAndDeliveryId(String tenantId, String deliveryId);
}