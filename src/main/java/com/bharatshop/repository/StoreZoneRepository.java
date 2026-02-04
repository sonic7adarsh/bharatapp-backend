package com.bharatshop.repository;

import com.bharatshop.entity.StoreZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreZoneRepository extends JpaRepository<StoreZoneEntity, String> {
    List<StoreZoneEntity> findByStoreId(String storeId);
    List<StoreZoneEntity> findByZoneId(String zoneId);
    List<StoreZoneEntity> findByZoneIdIn(java.util.Collection<String> zoneIds);
}