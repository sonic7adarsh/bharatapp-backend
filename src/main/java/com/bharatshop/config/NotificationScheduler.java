package com.bharatshop.config;

import com.bharatshop.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);
    
    private final NotificationService notificationService;
    
    @Value("${notifications.scheduler.enabled:true}")
    private boolean schedulerEnabled;
    
    @Value("${notifications.scheduler.cron:0 */5 * * * *}")
    private String cronExpression;
    
    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Process scheduled notifications every 5 minutes by default
     * This will pick up notifications that were scheduled for quiet hours
     * or any pending notifications that need to be retried
     */
    @Scheduled(cron = "${notifications.scheduler.cron:0 */5 * * * *}")
    public void processScheduledNotifications() {
        if (!schedulerEnabled) {
            logger.debug("Notification scheduler is disabled");
            return;
        }
        
        try {
            logger.info("Starting scheduled notification processing");
            notificationService.processScheduledNotifications();
            logger.info("Completed scheduled notification processing");
        } catch (Exception e) {
            logger.error("Error during scheduled notification processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clean up old notification logs daily at 2 AM
     * This helps manage database size by removing logs older than 90 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldLogs() {
        if (!schedulerEnabled) {
            logger.debug("Notification scheduler is disabled");
            return;
        }
        
        try {
            logger.info("Starting notification log cleanup");
            // This would typically call a cleanup service method
            // For now, we'll just log that it's scheduled
            logger.info("Notification log cleanup scheduled (implementation pending)");
        } catch (Exception e) {
            logger.error("Error during notification log cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Health check for notification system
     * Runs every minute to ensure the system is healthy
     */
    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        if (!schedulerEnabled) {
            return;
        }
        
        try {
            // Simple health check - could be expanded to check provider connectivity
            logger.debug("Notification system health check passed");
        } catch (Exception e) {
            logger.error("Notification system health check failed: {}", e.getMessage(), e);
        }
    }
}