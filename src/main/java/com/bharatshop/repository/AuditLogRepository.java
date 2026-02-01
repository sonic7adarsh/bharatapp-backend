package com.bharatshop.repository;

import com.bharatshop.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    // Basic queries
    List<AuditLog> findByTenantId(String tenantId);
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByActionStatus(String actionStatus);
    List<AuditLog> findByResourceType(String resourceType);
    List<AuditLog> findByResourceId(String resourceId);
    
    // Combined queries
    List<AuditLog> findByTenantIdAndUserId(String tenantId, String userId);
    List<AuditLog> findByTenantIdAndAction(String tenantId, String action);
    List<AuditLog> findByTenantIdAndActionStatus(String tenantId, String actionStatus);
    List<AuditLog> findByTenantIdAndResourceType(String tenantId, String resourceType);
    List<AuditLog> findByTenantIdAndResourceId(String tenantId, String resourceId);
    List<AuditLog> findByTenantIdAndUserIdAndAction(String tenantId, String userId, String action);
    
    // Time-based queries
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<AuditLog> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime startTime, LocalDateTime endTime);
    List<AuditLog> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Paginated queries
    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    Page<AuditLog> findByTenantIdAndAction(String tenantId, String action, Pageable pageable);
    Page<AuditLog> findByTenantIdAndActionStatus(String tenantId, String actionStatus, Pageable pageable);
    Page<AuditLog> findByTenantIdAndUserId(String tenantId, String userId, Pageable pageable);
    Page<AuditLog> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    // Search queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND " +
           "(LOWER(al.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.actionStatus) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<AuditLog> searchByTenantIdAndTerm(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND " +
           "(LOWER(al.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.actionStatus) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<AuditLog> searchByTenantIdAndTerm(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Aggregation queries
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId AND al.createdAt BETWEEN :startTime AND :endTime")
    long countByTenantIdAndTimeRange(@Param("tenantId") String tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId AND al.action = :action")
    long countByTenantIdAndAction(@Param("tenantId") String tenantId, @Param("action") String action);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId AND al.actionStatus = :status")
    long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);
    
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId GROUP BY al.action")
    List<Object[]> getActionCountsByTenantId(@Param("tenantId") String tenantId);
    
    @Query("SELECT al.actionStatus, COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId GROUP BY al.actionStatus")
    List<Object[]> getStatusCountsByTenantId(@Param("tenantId") String tenantId);
    
    @Query("SELECT al.userId, COUNT(al) FROM AuditLog al WHERE al.tenantId = :tenantId GROUP BY al.userId ORDER BY COUNT(al) DESC")
    List<Object[]> getTopUsersByActivity(@Param("tenantId") String tenantId, Pageable pageable);
    
    // Recent activity queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentActivityByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentActivityByUserId(@Param("userId") String userId, Pageable pageable);
    
    // Error tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.actionStatus = 'FAILURE' ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentFailuresByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.actionStatus = 'FAILURE' AND al.createdAt BETWEEN :startTime AND :endTime")
    List<AuditLog> findFailuresByTenantIdAndTimeRange(@Param("tenantId") String tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Security tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.action IN ('SECURITY_VIOLATION', 'RATE_LIMIT_EXCEEDED', 'UNAUTHORIZED_ACCESS') ORDER BY al.createdAt DESC")
    List<AuditLog> findSecurityEventsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
    
    // Resource tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.resourceType = :resourceType AND al.resourceId = :resourceId ORDER BY al.createdAt DESC")
    List<AuditLog> findByTenantIdAndResource(@Param("tenantId") String tenantId, @Param("resourceType") String resourceType, @Param("resourceId") String resourceId);
    
    // IP tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.ipAddress = :ipAddress ORDER BY al.createdAt DESC")
    List<AuditLog> findByTenantIdAndIpAddress(@Param("tenantId") String tenantId, @Param("ipAddress") String ipAddress);
    
    // Session tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.sessionId = :sessionId ORDER BY al.createdAt DESC")
    List<AuditLog> findByTenantIdAndSessionId(@Param("tenantId") String tenantId, @Param("sessionId") String sessionId);
    
    // Request tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.requestId = :requestId")
    Optional<AuditLog> findByTenantIdAndRequestId(@Param("tenantId") String tenantId, @Param("requestId") String requestId);
    
    // Execution time queries
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.executionTimeMs > :threshold ORDER BY al.executionTimeMs DESC")
    List<AuditLog> findSlowQueriesByTenantId(@Param("tenantId") String tenantId, @Param("threshold") Long threshold);
    
    // Cleanup queries
    @Query("DELETE FROM AuditLog al WHERE al.createdAt < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("DELETE FROM AuditLog al WHERE al.tenantId = :tenantId AND al.createdAt < :cutoffDate")
    void deleteOldLogsByTenantId(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDateTime cutoffDate);
}