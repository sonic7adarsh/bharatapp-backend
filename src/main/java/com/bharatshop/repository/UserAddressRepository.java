package com.bharatshop.repository;

import com.bharatshop.entity.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, String> {
    List<UserAddressEntity> findByUserId(String userId);
}
