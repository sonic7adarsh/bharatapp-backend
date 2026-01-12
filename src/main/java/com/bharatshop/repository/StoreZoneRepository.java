package com.bharatshop.repository;

import com.bharatshop.entity.StoreZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreZoneRepository extends JpaRepository<StoreZoneEntity, String> {
    List<StoreZoneEntity> findByTenantIdAndStoreId(String tenantId, String storeId);
}