package com.bharatshop.repository;

import com.bharatshop.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ZoneRepository extends JpaRepository<ZoneEntity, String> {
    List<ZoneEntity> findByTenantId(String tenantId);
}