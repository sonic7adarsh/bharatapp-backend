package com.bharatshop.repository;

import com.bharatshop.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface StoreRepository extends JpaRepository<StoreEntity, String> {
    List<StoreEntity> findByCategoryIgnoreCase(String category);
    List<StoreEntity> findByNameContainingIgnoreCase(String name);
    List<StoreEntity> findByOwnerId(String ownerId);
    List<StoreEntity> findByOwnerPhone(String ownerPhone);

    @Query(value = "SELECT * FROM stores s WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) < :radius", nativeQuery = true)
    List<StoreEntity> findNearbyStores(@Param("lat") Double lat, @Param("lng") Double lng, @Param("radius") Double radius);
}