package com.bharatshop.repository;

import com.bharatshop.entity.RiderZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderZoneRepository extends JpaRepository<RiderZoneEntity, String> {
    List<RiderZoneEntity> findByRiderId(String riderId);
    List<RiderZoneEntity> findByZoneId(String zoneId);
}