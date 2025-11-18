package com.bharatshop.repository;

import com.bharatshop.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<BookingEntity, String> {
    List<BookingEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}