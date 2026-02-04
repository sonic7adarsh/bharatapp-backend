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
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByActionStatus(String actionStatus);
    List<AuditLog> findByResourceType(String resourceType);
    List<AuditLog> findByResourceId(String resourceId);
    
    // Combined queries
    List<AuditLog> findByUserIdAndAction(String userId, String action);
    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);
    List<AuditLog> findByUserIdAndActionAndActionStatus(String userId, String action, String actionStatus);
    
    // Time-based queries
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<AuditLog> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Paginated queries
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    Page<AuditLog> findByActionStatus(String actionStatus, Pageable pageable);
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    // Search queries
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(LOWER(al.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.actionStatus) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<AuditLog> searchByTerm(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(LOWER(al.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.resourceId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(al.actionStatus) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<AuditLog> searchByTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Aggregation queries
    @Query("SELECT COUNT(al) FROM AuditLog al")
    long countAll();
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.createdAt BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.action = :action")
    long countByAction(@Param("action") String action);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.actionStatus = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al GROUP BY al.action")
    List<Object[]> getActionCounts();
    
    @Query("SELECT al.actionStatus, COUNT(al) FROM AuditLog al GROUP BY al.actionStatus")
    List<Object[]> getStatusCounts();
    
    @Query("SELECT al.userId, COUNT(al) FROM AuditLog al GROUP BY al.userId ORDER BY COUNT(al) DESC")
    List<Object[]> getTopUsersByActivity(Pageable pageable);
    
    // Recent activity queries
    @Query("SELECT al FROM AuditLog al ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentActivity(Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentActivityByUserId(@Param("userId") String userId, Pageable pageable);
    
    // Error tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.actionStatus = 'FAILURE' ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentFailures(Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.actionStatus = 'FAILURE' AND al.createdAt BETWEEN :startTime AND :endTime")
    List<AuditLog> findFailuresByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Security tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.action IN ('SECURITY_VIOLATION', 'RATE_LIMIT_EXCEEDED', 'UNAUTHORIZED_ACCESS') ORDER BY al.createdAt DESC")
    List<AuditLog> findSecurityEvents(Pageable pageable);
    
    // Resource tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.resourceType = :resourceType AND al.resourceId = :resourceId ORDER BY al.createdAt DESC")
    List<AuditLog> findByResource(@Param("resourceType") String resourceType, @Param("resourceId") String resourceId);
    
    // IP tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress ORDER BY al.createdAt DESC")
    List<AuditLog> findByIpAddress(@Param("ipAddress") String ipAddress);
    
    // Session tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.sessionId = :sessionId ORDER BY al.createdAt DESC")
    List<AuditLog> findBySessionId(@Param("sessionId") String sessionId);
    
    // Request tracking queries
    @Query("SELECT al FROM AuditLog al WHERE al.requestId = :requestId")
    Optional<AuditLog> findByRequestId(@Param("requestId") String requestId);
    
    // Execution time queries
    @Query("SELECT al FROM AuditLog al WHERE al.executionTimeMs > :threshold ORDER BY al.executionTimeMs DESC")
    List<AuditLog> findSlowQueries(@Param("threshold") Long threshold);
    
    // Cleanup queries
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM AuditLog al WHERE al.createdAt < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
