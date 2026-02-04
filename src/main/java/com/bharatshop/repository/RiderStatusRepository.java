package com.bharatshop.repository;

import com.bharatshop.entity.RiderStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiderStatusRepository extends JpaRepository<RiderStatusEntity, String> {
    List<RiderStatusEntity> findByRiderIdOrderByTsDesc(String riderId);
}