package com.bharatshop.repository;

import com.bharatshop.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    
    List<UserRoleEntity> findByUserId(String userId);
    
    Optional<UserRoleEntity> findByUserIdAndRole(String userId, String role);
    
    Optional<UserRoleEntity> findByUserIdAndIsActiveTrue(String userId);
    
    @Query("SELECT ur.role FROM UserRoleEntity ur WHERE ur.userId = :userId")
    List<String> findRolesByUserId(@Param("userId") String userId);
    
    @Query("SELECT ur FROM UserRoleEntity ur WHERE ur.userId = :userId AND ur.isActive = true")
    Optional<UserRoleEntity> findActiveRoleByUserId(@Param("userId") String userId);
    
    void deleteByUserIdAndRole(String userId, String role);
}