package com.bharatshop.repository;

import com.bharatshop.entity.DeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttemptEntity, String> {
    List<DeliveryAttemptEntity> findByDeliveryId(String deliveryId);
}