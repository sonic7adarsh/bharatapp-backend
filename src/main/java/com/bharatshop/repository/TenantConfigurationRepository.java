package com.bharatshop.repository;

import com.bharatshop.entity.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, String> {
    
    Optional<TenantConfiguration> findByTenantIdAndConfigKey(String tenantId, String configKey);
    
    List<TenantConfiguration> findByTenantId(String tenantId);
    
    List<TenantConfiguration> findByTenantIdAndIsFeatureFlagTrue(String tenantId);
    
    List<TenantConfiguration> findByTenantIdAndConfigKeyContaining(String tenantId, String keyPattern);
    
    @Query("SELECT tc FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId AND tc.isFeatureFlag = true AND tc.configValue = 'true'")
    List<TenantConfiguration> findEnabledFeatures(@Param("tenantId") String tenantId);
    
    @Query("SELECT tc FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId AND tc.configKey LIKE %:searchTerm%")
    List<TenantConfiguration> searchByKey(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(tc) FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
    
    @Query("SELECT COUNT(tc) FROM TenantConfiguration tc WHERE tc.tenantId = :tenantId AND tc.isFeatureFlag = true")
    long countFeatureFlagsByTenantId(@Param("tenantId") String tenantId);
    
    boolean existsByTenantIdAndConfigKey(String tenantId, String configKey);
    
    void deleteByTenantIdAndConfigKey(String tenantId, String configKey);
    
    void deleteByTenantId(String tenantId);
}