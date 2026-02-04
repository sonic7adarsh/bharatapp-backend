package com.bharatshop.repository;

import com.bharatshop.entity.OrderDeliveryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface OrderDeliveryRepository extends JpaRepository<OrderDeliveryEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      SELECT d FROM OrderDeliveryEntity d
      WHERE d.deliveryId = :deliveryId
        AND d.status = 'READY'
    """)
    Optional<OrderDeliveryEntity> lockReadyForAssign(String deliveryId);

    List<OrderDeliveryEntity> findByStatus(String status);

    List<OrderDeliveryEntity> findByOrderId(String orderId);

    List<OrderDeliveryEntity> findByRiderIdAndStatusIn(String riderId, List<String> statuses);

    // COPY-PASTE (EXACT) Active / Completed queries
    @Query("""
      SELECT d FROM OrderDeliveryEntity d
      WHERE d.riderId = :riderId
        AND d.status IN ('RIDER_ASSIGNED','PICKED_UP','OUT_FOR_DELIVERY')
    """)
    List<OrderDeliveryEntity> findActive(String riderId);

    @Query("""
      SELECT d FROM OrderDeliveryEntity d
      WHERE d.riderId = :riderId
        AND d.status = 'DELIVERED'
    """)
    List<OrderDeliveryEntity> findCompleted(String riderId);
}
