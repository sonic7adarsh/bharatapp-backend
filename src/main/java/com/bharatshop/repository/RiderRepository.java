package com.bharatshop.repository;

import com.bharatshop.entity.RiderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RiderRepository extends JpaRepository<RiderEntity, String> {
    List<RiderEntity> findByStatus(String status);
    Optional<RiderEntity> findByPhone(String phone);
}