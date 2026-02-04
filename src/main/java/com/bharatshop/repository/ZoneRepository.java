package com.bharatshop.repository;

import com.bharatshop.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ZoneRepository extends JpaRepository<ZoneEntity, String> {
    
    @Query("SELECT z FROM ZoneEntity z WHERE " +
           "( (z.type = 'radius' AND " +
           "   (6371 * acos(cos(radians(:lat)) * cos(radians(z.centerLat)) * cos(radians(z.centerLng) - radians(:lng)) + " +
           "   sin(radians(:lat)) * sin(radians(z.centerLat)))) * 1000 <= z.radiusMeters) " +
           " OR z.type = 'polygon' )")
    List<ZoneEntity> findNearbyZones(@Param("lat") Double lat, 
                                     @Param("lng") Double lng);
}