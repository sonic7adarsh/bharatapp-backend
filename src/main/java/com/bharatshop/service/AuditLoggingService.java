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
    
    public AuditLog logAction(String userId, String action, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                userId,
                action,
                actionStatus
            );
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log action: user={}, action={}, status={}", 
                userId, action, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logResourceAction(String userId, String action, 
                                    String resourceType, String resourceId, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                userId,
                action,
                actionStatus
            );
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log resource action: user={}, action={}, resourceType={}, resourceId={}, status={}", 
                userId, action, resourceType, resourceId, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logUserAction(String userId, String userEmail, String userRole,
                                String action, String actionStatus, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
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
            logger.error("Failed to log user action: user={}, action={}, status={}", 
                userId, action, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logStateChange(String userId, String action, String resourceType, 
                                 String resourceId, Object beforeState, Object afterState, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
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
            logger.error("Failed to log state change: user={}, action={}, resourceType={}, resourceId={}, status={}", 
                userId, action, resourceType, resourceId, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logError(String userId, String action, String resourceType, 
                           String resourceId, String errorMessage, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                userId,
                action,
                actionStatus
            );
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setErrorMessage(errorMessage);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log error: user={}, action={}, resourceType={}, resourceId={}, error={}, status={}", 
                userId, action, resourceType, resourceId, errorMessage, actionStatus, e);
            return null;
        }
    }
    
    public AuditLog logSecurityEvent(String userId, String action, String ipAddress, 
                                   String userAgent, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                userId,
                action,
                AuditLog.ActionStatus.FAILURE.getValue()
            );
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setErrorMessage(errorMessage);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log security event: user={}, action={}, ipAddress={}", 
                userId, action, ipAddress, e);
            return null;
        }
    }
    
    public AuditLog logPerformance(String userId, String action, long executionTimeMs, 
                                 String resourceType, String resourceId, String actionStatus) {
        try {
            AuditLog auditLog = new AuditLog(
                UUID.randomUUID().toString(),
                userId,
                action,
                actionStatus
            );
            auditLog.setExecutionTimeMs(executionTimeMs);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log performance: user={}, action={}, executionTimeMs={}, status={}", 
                userId, action, executionTimeMs, actionStatus, e);
            return null;
        }
    }
    
    // Query methods
    public List<AuditLog> getAuditLogsByUser(String userId) {
        return auditLogRepository.findByUserId(userId);
    }
    
    public Page<AuditLog> getAuditLogsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findAll(pageable);
    }
    
    public Optional<AuditLog> getAuditLog(String id) {
        return auditLogRepository.findById(id);
    }
    
    public Page<AuditLog> searchAuditLogsPaginated(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.searchByTerm(query, pageable);
    }
    
    public List<AuditLog> getRecentActivity(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findRecentActivity(pageable);
    }
    
    public List<AuditLog> getRecentFailures(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findRecentFailures(pageable);
    }
    
    public List<AuditLog> getSecurityEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findSecurityEvents(pageable);
    }
    
    public List<AuditLog> getSlowQueries(long thresholdMs) {
        return auditLogRepository.findSlowQueries(thresholdMs);
    }

    // Analytics methods
    public Map<String, Long> getActionCounts() {
        List<Object[]> results = auditLogRepository.getActionCounts();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }
    
    public Map<String, Long> getStatusCounts() {
        List<Object[]> results = auditLogRepository.getStatusCounts();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }
    
    public List<Object[]> getTopUsersByActivity(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.getTopUsersByActivity(pageable);
    }
    
    public long getTotalLogsCount() {
        return auditLogRepository.countAll();
    }
    
    public long getLogsCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.countByTimeRange(startTime, endTime);
    }
    
    public long getLogsCountByAction(String action) {
        return auditLogRepository.countByAction(action);
    }
    
    public long getLogsCountByStatus(String status) {
        return auditLogRepository.countByStatus(status);
    }
    
    public List<AuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByCreatedAtBetween(startTime, endTime);
    }
    
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
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
}