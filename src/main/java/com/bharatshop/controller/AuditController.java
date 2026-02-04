package com.bharatshop.controller;

import com.bharatshop.entity.AuditLog;
import com.bharatshop.service.AuditLoggingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
    
    @Autowired
    private AuditLoggingService auditLoggingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            Page<AuditLog> logs;
            
            if (startDate != null && endDate != null) {
                logs = auditLoggingService.getAuditLogsPaginated(page, size);
                // Filter by date range manually since we need to combine with other filters
                List<AuditLog> filteredLogs = logs.getContent().stream()
                    .filter(log -> log.getCreatedAt().isAfter(startDate) && log.getCreatedAt().isBefore(endDate))
                    .collect(Collectors.toList());
                
                Map<String, Object> response = new HashMap<>();
                response.put("logs", filteredLogs);
                response.put("totalElements", filteredLogs.size());
                response.put("totalPages", 1);
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasNext", false);
                response.put("hasPrevious", page > 0);
                
                return ResponseEntity.ok(response);
            } else {
                logs = auditLoggingService.getAuditLogsPaginated(page, size);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("currentPage", logs.getNumber());
            response.put("size", logs.getSize());
            response.put("hasNext", logs.hasNext());
            response.put("hasPrevious", logs.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get audit logs"));
        }
    }
    
    @GetMapping("/logs/search")
    public ResponseEntity<Map<String, Object>> searchAuditLogs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<AuditLog> logs = auditLoggingService.searchAuditLogsPaginated(query, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("currentPage", logs.getNumber());
            response.put("size", logs.getSize());
            response.put("hasNext", logs.hasNext());
            response.put("hasPrevious", logs.hasPrevious());
            response.put("query", query);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching audit logs with query: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to search audit logs"));
        }
    }
    
    @GetMapping("/logs/{logId}")
    public ResponseEntity<Map<String, Object>> getAuditLog(
            @PathVariable String logId) {
        try {
            AuditLog log = auditLoggingService.getAuditLog(logId).orElse(null);
            
            if (log != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("log", log);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Audit log not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting audit log {}", logId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get audit log"));
        }
    }
    
    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAuditSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            Map<String, Object> summary = new HashMap<>();
            
            // Total logs count
            long totalLogs;
            if (startDate != null && endDate != null) {
                totalLogs = auditLoggingService.getLogsCountByTimeRange(startDate, endDate);
            } else {
                totalLogs = auditLoggingService.getTotalLogsCount();
            }
            summary.put("totalLogs", totalLogs);
            
            // Action counts
            Map<String, Long> actionCounts = auditLoggingService.getActionCounts();
            summary.put("actionCounts", actionCounts);
            
            // Status counts
            Map<String, Long> statusCounts = auditLoggingService.getStatusCounts();
            summary.put("statusCounts", statusCounts);
            
            // Recent activity (last 10)
            List<AuditLog> recentActivity = auditLoggingService.getRecentActivity(10);
            summary.put("recentActivity", recentActivity);
            
            // Recent failures (last 10)
            List<AuditLog> recentFailures = auditLoggingService.getRecentFailures(10);
            summary.put("recentFailures", recentFailures);
            
            // Security events (last 10)
            List<AuditLog> securityEvents = auditLoggingService.getSecurityEvents(10);
            summary.put("securityEvents", securityEvents);
            
            // Top active users (top 10)
            List<Object[]> topUsers = auditLoggingService.getTopUsersByActivity(10);
            summary.put("topUsers", topUsers);
            
            // Slow queries (execution time > 5 seconds)
            List<AuditLog> slowQueries = auditLoggingService.getSlowQueries(5000L);
            summary.put("slowQueries", slowQueries);
            
            if (startDate != null) summary.put("startDate", startDate);
            if (endDate != null) summary.put("endDate", endDate);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting audit summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get audit summary"));
        }
    }
    
    @GetMapping("/analytics/actions")
    public ResponseEntity<Map<String, Object>> getActionAnalytics(
            @RequestParam(required = false) String action) {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            if (action != null) {
                long count = auditLoggingService.getLogsCountByAction(action);
                analytics.put("action", action);
                analytics.put("count", count);
            } else {
                Map<String, Long> actionCounts = auditLoggingService.getActionCounts();
                analytics.put("allActions", actionCounts);
            }
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.error("Error getting action analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get action analytics"));
        }
    }
    
    @GetMapping("/analytics/statuses")
    public ResponseEntity<Map<String, Object>> getStatusAnalytics(
            @RequestParam(required = false) String status) {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            if (status != null) {
                long count = auditLoggingService.getLogsCountByStatus(status);
                analytics.put("status", status);
                analytics.put("count", count);
            } else {
                Map<String, Long> statusCounts = auditLoggingService.getStatusCounts();
                analytics.put("allStatuses", statusCounts);
            }
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.error("Error getting status analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get status analytics"));
        }
    }
    
    @GetMapping("/compliance/export")
    public ResponseEntity<Map<String, Object>> exportComplianceData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<AuditLog> logs;
            if (startDate != null && endDate != null) {
                logs = auditLoggingService.getAuditLogsByTimeRange(startDate, endDate);
            } else {
                logs = auditLoggingService.getAllAuditLogs();
            }
            
            // Generate compliance report
            Map<String, Object> complianceReport = new HashMap<>();
            complianceReport.put("exportDate", LocalDateTime.now());
            complianceReport.put("totalRecords", logs.size());
            
            if (startDate != null) complianceReport.put("startDate", startDate);
            if (endDate != null) complianceReport.put("endDate", endDate);
            
            // Summary statistics
            Map<String, Long> actionCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
            complianceReport.put("actionSummary", actionCounts);
            
            Map<String, Long> statusCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getActionStatus, Collectors.counting()));
            complianceReport.put("statusSummary", statusCounts);
            
            // Security events summary
            List<AuditLog> securityEvents = logs.stream()
                .filter(log -> Arrays.asList("SECURITY_VIOLATION", "RATE_LIMIT_EXCEEDED", "UNAUTHORIZED_ACCESS").contains(log.getAction()))
                .collect(Collectors.toList());
            complianceReport.put("securityEventsCount", securityEvents.size());
            complianceReport.put("securityEvents", securityEvents);
            
            // Error summary
            List<AuditLog> errors = logs.stream()
                .filter(log -> "FAILURE".equals(log.getActionStatus()) && log.getErrorMessage() != null)
                .collect(Collectors.toList());
            complianceReport.put("errorCount", errors.size());
            complianceReport.put("errors", errors);
            
            return ResponseEntity.ok(complianceReport);
        } catch (Exception e) {
            logger.error("Error exporting compliance data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to export compliance data"));
        }
    }
    
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoffDate) {
        try {
            auditLoggingService.cleanupOldLogs(cutoffDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Old audit logs cleaned up successfully");
            response.put("cutoffDate", cutoffDate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error cleaning up old audit logs with cutoff date {}", cutoffDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to cleanup old audit logs"));
        }
    }
}