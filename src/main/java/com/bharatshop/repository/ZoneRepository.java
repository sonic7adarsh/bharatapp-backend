package com.bharatshop.repository;

import com.bharatshop.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ZoneRepository extends JpaRepository<ZoneEntity, String> {
    List<ZoneEntity> findByTenantId(String tenantId);
    java.util.Optional<ZoneEntity> findByTenantIdAndId(String tenantId, String id);

    @Query("SELECT z FROM ZoneEntity z WHERE z.tenantId = :tenantId AND " +
           "( (z.type = 'radius' AND " +
           "   (6371 * acos(cos(radians(:lat)) * cos(radians(z.centerLat)) * cos(radians(z.centerLng) - radians(:lng)) + " +
           "   sin(radians(:lat)) * sin(radians(z.centerLat)))) * 1000 <= z.radiusMeters) " +
           " OR z.type = 'polygon' )")
    List<ZoneEntity> findNearbyZones(@Param("tenantId") String tenantId, 
                                     @Param("lat") Double lat, 
                                     @Param("lng") Double lng);
}