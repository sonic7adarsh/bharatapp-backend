package com.bharatshop.service;

import com.bharatshop.entity.AuditLog;
import com.bharatshop.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class AuditLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Async
    public CompletableFuture<AuditLog> logAction(AuditLog auditLog) {
        try {
            AuditLog saved = auditLogRepository.save(auditLog);
            return CompletableFuture.completedFuture(saved);
        } catch (Exception e) {
            logger.error("Failed to save audit log: {}", auditLog, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public AuditLog logAction(String tenantId, String userId, String action, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                actionStatus
            );
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log action: tenant={}, user={}, action={}, status={}", 
                tenantId, userId, action, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logResourceAction(String tenantId, String userId, String action, 
                                    String resourceType, String resourceId, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                actionStatus
            );
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log resource action: tenant={}, user={}, action={}, resourceType={}, resourceId={}, status={}", 
                tenantId, userId, action, resourceType, resourceId, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logUserAction(String tenantId, String userId, String userEmail, String userRole,
                                String action, String actionStatus, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                actionStatus
            );
            auditLog.setUserEmail(userEmail);
            auditLog.setUserRole(userRole);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log user action: tenant={}, user={}, action={}, status={}", 
                tenantId, userId, action, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logStateChange(String tenantId, String userId, String action, String resourceType, 
                                 String resourceId, Object beforeState, Object afterState, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                actionStatus
            );
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            
            if (beforeState != null) {
                auditLog.setBeforeState(objectMapper.writeValueAsString(beforeState));
            }
            if (afterState != null) {
                auditLog.setAfterState(objectMapper.writeValueAsString(afterState));
            }
            
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log state change: tenant={}, user={}, action={}, resourceType={}, resourceId={}, status={}", 
                tenantId, userId, action, resourceType, resourceId, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logError(String tenantId, String userId, String action, String resourceType, 
                           String resourceId, String errorMessage, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                actionStatus
            );
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setErrorMessage(errorMessage);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log error: tenant={}, user={}, action={}, resourceType={}, resourceId={}, error={}, status={}", 
                tenantId, userId, action, resourceType, resourceId, errorMessage, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logSecurityEvent(String tenantId, String userId, String action, String ipAddress, 
                                   String userAgent, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                AuditLog.ActionStatus.FAILURE.getValue()
            );
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setErrorMessage(errorMessage);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log security event: tenant={}, user={}, action={}, ipAddress={}", 
                tenantId, userId, action, ipAddress, e);
            return null;
        }
    }
    
    public AuditLog logPerformance(String tenantId, String userId, String action, long executionTimeMs, 
                                 String resourceType, String resourceId, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                action,
                actionStatus
            );
            auditLog.setExecutionTimeMs(executionTimeMs);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log performance: tenant={}, user={}, action={}, executionTimeMs={}, status={}", 
                tenantId, userId, action, executionTimeMs, actionStatus, e);
            return null;
        }
    }
    
    // Query methods
    public List<AuditLog> getAuditLogsByTenant(String tenantId) {
        return auditLogRepository.findByTenantId(tenantId);
    }
    
    public List<AuditLog> getAuditLogsByUser(String userId) {
        return auditLogRepository.findByUserId(userId);
    }
    
    public List<AuditLog> getAuditLogsByTenantAndUser(String tenantId, String userId) {
        return auditLogRepository.findByTenantIdAndUserId(tenantId, userId);
    }
    
    public List<AuditLog> getAuditLogsByAction(String tenantId, String action) {
        return auditLogRepository.findByTenantIdAndAction(tenantId, action);
    }
    
    public List<AuditLog> getAuditLogsByStatus(String tenantId, String status) {
        return auditLogRepository.findByTenantIdAndActionStatus(tenantId, status);
    }
    
    public List<AuditLog> getAuditLogsByResource(String tenantId, String resourceType, String resourceId) {
        return auditLogRepository.findByTenantIdAndResource(tenantId, resourceType, resourceId);
    }
    
    public List<AuditLog> getAuditLogsByTimeRange(String tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, startTime, endTime);
    }
    
    public Page<AuditLog> getAuditLogsPaginated(String tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findByTenantId(tenantId, pageable);
    }
    
    public List<AuditLog> getRecentActivity(String tenantId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findRecentActivityByTenantId(tenantId, pageable);
    }
    
    public List<AuditLog> getRecentFailures(String tenantId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findRecentFailuresByTenantId(tenantId, pageable);
    }
    
    public List<AuditLog> getSecurityEvents(String tenantId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findSecurityEventsByTenantId(tenantId, pageable);
    }
    
    public List<AuditLog> getSlowQueries(String tenantId, long thresholdMs) {
        return auditLogRepository.findSlowQueriesByTenantId(tenantId, thresholdMs);
    }
    
    public List<AuditLog> searchAuditLogs(String tenantId, String searchTerm) {
        return auditLogRepository.searchByTenantIdAndTerm(tenantId, searchTerm);
    }
    
    public Page<AuditLog> searchAuditLogsPaginated(String tenantId, String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.searchByTenantIdAndTerm(tenantId, searchTerm, pageable);
    }
    
    // Analytics methods
    public Map<String, Long> getActionCounts(String tenantId) {
        List<Object[]> results = auditLogRepository.getActionCountsByTenantId(tenantId);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }
    
    public Map<String, Long> getStatusCounts(String tenantId) {
        List<Object[]> results = auditLogRepository.getStatusCountsByTenantId(tenantId);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }
    
    public List<Object[]> getTopUsersByActivity(String tenantId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.getTopUsersByActivity(tenantId, pageable);
    }
    
    public long getTotalLogsCount(String tenantId) {
        return auditLogRepository.countByTenantId(tenantId);
    }
    
    public long getLogsCountByTimeRange(String tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.countByTenantIdAndTimeRange(tenantId, startTime, endTime);
    }
    
    public long getLogsCountByAction(String tenantId, String action) {
        return auditLogRepository.countByTenantIdAndAction(tenantId, action);
    }
    
    public long getLogsCountByStatus(String tenantId, String status) {
        return auditLogRepository.countByTenantIdAndStatus(tenantId, status);
    }
    
    // Cleanup methods
    public void cleanupOldLogs(LocalDateTime cutoffDate) {
        try {
            auditLogRepository.deleteOldLogs(cutoffDate);
            logger.info("Cleaned up audit logs older than {}", cutoffDate);
        } catch (Exception e) {
            logger.error("Failed to cleanup old audit logs", e);
        }
    }
    
    public void cleanupOldLogsByTenant(String tenantId, LocalDateTime cutoffDate) {
        try {
            auditLogRepository.deleteOldLogsByTenantId(tenantId, cutoffDate);
            logger.info("Cleaned up audit logs for tenant {} older than {}", tenantId, cutoffDate);
        } catch (Exception e) {
            logger.error("Failed to cleanup old audit logs for tenant {}", tenantId, e);
        }
    }
}