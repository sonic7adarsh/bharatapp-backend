package com.bharatshop.repository;

import com.bharatshop.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<StoreEntity, String> {
    List<StoreEntity> findByCategoryIgnoreCase(String category);
    List<StoreEntity> findByNameContainingIgnoreCase(String name);
    List<StoreEntity> findByOwnerId(String ownerId);
    List<StoreEntity> findByOwnerPhone(String ownerPhone);
}