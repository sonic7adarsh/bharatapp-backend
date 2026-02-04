package com.bharatshop.repository;

import com.bharatshop.entity.RiderLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiderLocationRepository extends JpaRepository<RiderLocationEntity, String> {
    List<RiderLocationEntity> findByRiderId(String riderId);
}