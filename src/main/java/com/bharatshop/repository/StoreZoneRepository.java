package com.bharatshop.repository;

import com.bharatshop.entity.StoreZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreZoneRepository extends JpaRepository<StoreZoneEntity, String> {
    List<StoreZoneEntity> findByTenantIdAndStoreId(String tenantId, String storeId);
    List<StoreZoneEntity> findByTenantIdAndZoneId(String tenantId, String zoneId);
    List<StoreZoneEntity> findByTenantIdAndZoneIdIn(String tenantId, java.util.Collection<String> zoneIds);
}