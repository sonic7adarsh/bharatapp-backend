package com.bharatshop.repository;

import com.bharatshop.entity.RiderZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderZoneRepository extends JpaRepository<RiderZoneEntity, String> {
    List<RiderZoneEntity> findByTenantIdAndRiderId(String tenantId, String riderId);
    List<RiderZoneEntity> findByTenantIdAndZoneId(String tenantId, String zoneId);
}