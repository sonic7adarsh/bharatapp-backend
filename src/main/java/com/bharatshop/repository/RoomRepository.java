package com.bharatshop.repository;

import com.bharatshop.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomRepository extends JpaRepository<RoomEntity, String> {
    List<RoomEntity> findByStoreIdAndActiveIsTrueOrderByCreatedAtDesc(String storeId);
}