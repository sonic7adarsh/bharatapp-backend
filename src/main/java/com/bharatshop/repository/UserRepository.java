package com.bharatshop.repository;

import com.bharatshop.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByPhone(String phone);

    // Tenant-scoped queries (MVP requirement)
    Optional<UserEntity> findByEmailAndTenantId(String email, String tenantId);
    Optional<UserEntity> findByIdAndTenantId(String id, String tenantId);
    boolean existsByEmailAndTenantId(String email, String tenantId);
    // Added to prevent NonUniqueResultException when same phone exists across tenants
    java.util.List<UserEntity> findByTenantIdAndPhone(String tenantId, String phone);
}