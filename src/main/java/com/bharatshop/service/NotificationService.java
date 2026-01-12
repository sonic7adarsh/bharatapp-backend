package com.bharatshop.service;

import com.bharatshop.entity.NotificationEvent;
import com.bharatshop.entity.NotificationLog;
import com.bharatshop.entity.NotificationTemplate;
import com.bharatshop.entity.UserNotificationPreference;
import com.bharatshop.repository.NotificationEventRepository;
import com.bharatshop.repository.NotificationLogRepository;
import com.bharatshop.repository.NotificationTemplateRepository;
import com.bharatshop.repository.UserNotificationPreferenceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationEventRepository eventRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final WhatsAppService whatsAppService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    
    @Value("${notifications.whatsapp.enabled:true}")
    private boolean whatsappEnabled;
    
    @Value("${notifications.sms.enabled:true}")
    private boolean smsEnabled;
    
    @Value("${notifications.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${notifications.default-language:en}")
    private String defaultLanguage;
    
    @Value("${notifications.quiet-hours.start:22:00}")
    private String quietHoursStart;
    
    @Value("${notifications.quiet-hours.end:08:00}")
    private String quietHoursEnd;
    
    @Value("${notifications.provider-priority:WHATSAPP,SMS,EMAIL}")
    private String providerPriority;

    public NotificationService(
            NotificationEventRepository eventRepository,
            NotificationLogRepository logRepository,
            NotificationTemplateRepository templateRepository,
            UserNotificationPreferenceRepository preferenceRepository,
            WhatsAppService whatsAppService,
            SmsService smsService,
            EmailService emailService,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.whatsAppService = whatsAppService;
        this.smsService = smsService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Send notification to user for a specific event
     */
    public CompletableFuture<Void> sendNotification(String tenantId, String userId, String eventType, Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create notification event
                NotificationEvent event = createNotificationEvent(tenantId, userId, eventType, data);
                
                // Process notification based on user preferences
                processNotificationEvent(event);
                
            } catch (Exception e) {
                logger.error("Failed to send notification for event {} to user {}: {}", eventType, userId, e.getMessage(), e);
            }
        });
    }

    /**
     * Send OTP notification
     */
    public CompletableFuture<Void> sendOtp(String tenantId, String userId, String phone, String otp, String otpType) {
        Map<String, Object> data = new HashMap<>();
        data.put("otp", otp);
        data.put("otpType", otpType);
        data.put("phone", phone);
        
        String eventType = "OTP_" + otpType.toUpperCase();
        return sendNotification(tenantId, userId, eventType, data);
    }

    /**
     * Send order notification
     */
    public CompletableFuture<Void> sendOrderNotification(String tenantId, String userId, String orderId, String orderStatus, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderStatus", orderStatus);
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        String eventType = "ORDER_" + orderStatus.toUpperCase();
        return sendNotification(tenantId, userId, eventType, data);
    }

    /**
     * Send seller notification
     */
    public CompletableFuture<Void> sendSellerNotification(String tenantId, String sellerId, String eventType, Map<String, Object> data) {
        return sendNotification(tenantId, sellerId, eventType, data);
    }

    private NotificationEvent createNotificationEvent(String tenantId, String userId, String eventType, Map<String, Object> data) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTenantId(tenantId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEventData(data);
        event.setStatus("PENDING");
        event.setPriority(determinePriority(eventType));
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        
        // Check if we should schedule for quiet hours
        if (shouldScheduleForQuietHours()) {
            event.setScheduledAt(calculateNextDeliveryTime());
        }
        
        return eventRepository.save(event);
    }

    private void processNotificationEvent(NotificationEvent event) {
        try {
            // Get user preferences
            List<UserNotificationPreference> preferences = preferenceRepository.findByTenantIdAndUserId(
                event.getTenantId(), event.getUserId());
            
            if (preferences.isEmpty()) {
                // Create default preferences if none exist
                createDefaultPreferences(event.getTenantId(), event.getUserId());
                preferences = preferenceRepository.findByTenantIdAndUserId(
                    event.getTenantId(), event.getUserId());
            }
            
            // Filter preferences by enabled channels and quiet hours
            List<UserNotificationPreference> activePreferences = preferences.stream()
                .filter(pref -> pref.isEnabled() && !isQuietHoursForPreference(pref))
                .toList();
            
            if (activePreferences.isEmpty()) {
                logger.info("No active notification preferences for user {} in tenant {}", 
                    event.getUserId(), event.getTenantId());
                event.setStatus("SKIPPED");
                event.setProcessedAt(LocalDateTime.now());
                eventRepository.save(event);
                return;
            }
            
            // Try to send notification through available channels in priority order
            boolean sent = false;
            for (String channel : getProviderPriority()) {
                Optional<UserNotificationPreference> preference = activePreferences.stream()
                    .filter(pref -> pref.getChannel().equals(channel))
                    .findFirst();
                
                if (preference.isPresent()) {
                    sent = sendNotificationThroughChannel(event, channel, preference.get().getLanguage());
                    if (sent) {
                        break;
                    }
                }
            }
            
            // Update event status
            event.setStatus(sent ? "SENT" : "FAILED");
            event.setProcessedAt(LocalDateTime.now());
            eventRepository.save(event);
            
        } catch (Exception e) {
            logger.error("Error processing notification event {}: {}", event.getId(), e.getMessage(), e);
            event.setStatus("ERROR");
            event.setProcessedAt(LocalDateTime.now());
            eventRepository.save(event);
        }
    }

    private boolean sendNotificationThroughChannel(NotificationEvent event, String channel, String language) {
        try {
            // Get template for this event and channel
            Optional<NotificationTemplate> templateOpt = templateRepository
                .findByTenantIdAndEventTypeAndChannelAndLanguageAndIsActive(
                    event.getTenantId(), event.getEventType(), channel, language, true);
            
            if (templateOpt.isEmpty()) {
                // Try with default language
                templateOpt = templateRepository
                    .findByTenantIdAndEventTypeAndChannelAndLanguageAndIsActive(
                        event.getTenantId(), event.getEventType(), channel, defaultLanguage, true);
            }
            
            if (templateOpt.isEmpty()) {
                logger.warn("No template found for event {} channel {} language {}", 
                    event.getEventType(), channel, language);
                return false;
            }
            
            NotificationTemplate template = templateOpt.get();
            String message = processTemplate(template.getContent(), event.getEventData());
            String subject = template.getSubject() != null ? 
                processTemplate(template.getSubject(), event.getEventData()) : null;
            
            // Get recipient based on channel
            String recipient = getRecipientForChannel(event, channel);
            if (recipient == null) {
                logger.warn("No recipient found for user {} channel {}", event.getUserId(), channel);
                return false;
            }
            
            // Send through appropriate service
            boolean sent = switch (channel) {
                case "WHATSAPP" -> whatsappEnabled && whatsAppService.sendMessage(recipient, message);
                case "SMS" -> smsEnabled && smsService.sendMessage(recipient, message);
                case "EMAIL" -> emailEnabled && emailService.sendMessage(recipient, subject, message);
                default -> false;
            };
            
            // Log the notification attempt
            logNotification(event, channel, template.getProvider(), recipient, message, sent, null);
            
            return sent;
            
        } catch (Exception e) {
            logger.error("Error sending notification through channel {}: {}", channel, e.getMessage(), e);
            logNotification(event, channel, "UNKNOWN", null, null, false, e.getMessage());
            return false;
        }
    }

    private String processTemplate(String template, Map<String, Object> data) {
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String getRecipientForChannel(NotificationEvent event, String channel) {
        // This would typically come from user profile data
        // For now, we'll use the data map to store recipient info
        Map<String, Object> data = event.getEventData();
        return switch (channel) {
            case "WHATSAPP" -> (String) data.get("phone");
            case "SMS" -> (String) data.get("phone");
            case "EMAIL" -> (String) data.get("email");
            default -> null;
        };
    }

    private void logNotification(NotificationEvent event, String channel, String provider, 
                                String recipient, String message, boolean sent, String error) {
        try {
            NotificationLog log = new NotificationLog();
            log.setId(UUID.randomUUID().toString());
            log.setTenantId(event.getTenantId());
            log.setEventId(event.getId());
            log.setProvider(provider);
            log.setChannel(channel);
            log.setRecipient(recipient);
            log.setMessage(message);
            log.setStatus(sent ? "SENT" : "FAILED");
            log.setErrorMessage(error);
            log.setSentAt(LocalDateTime.now());
            log.setCreatedAt(LocalDateTime.now());
            
            logRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to log notification: {}", e.getMessage(), e);
        }
    }

    private void createDefaultPreferences(String tenantId, String userId) {
        String[] channels = {"WHATSAPP", "SMS", "EMAIL"};
        for (String channel : channels) {
            UserNotificationPreference preference = new UserNotificationPreference();
            preference.setId(UUID.randomUUID().toString());
            preference.setTenantId(tenantId);
            preference.setUserId(userId);
            preference.setChannel(channel);
            preference.setEnabled(true);
            preference.setQuietHoursStart(LocalTime.parse(quietHoursStart));
            preference.setQuietHoursEnd(LocalTime.parse(quietHoursEnd));
            preference.setLanguage(defaultLanguage);
            preference.setCreatedAt(LocalDateTime.now());
            preference.setUpdatedAt(LocalDateTime.now());
            
            preferenceRepository.save(preference);
        }
    }

    private boolean isQuietHoursForPreference(UserNotificationPreference preference) {
        LocalTime now = LocalTime.now();
        LocalTime start = preference.getQuietHoursStart();
        LocalTime end = preference.getQuietHoursEnd();
        
        if (start.isBefore(end)) {
            return now.isAfter(start) && now.isBefore(end);
        } else {
            return now.isAfter(start) || now.isBefore(end);
        }
    }

    private boolean shouldScheduleForQuietHours() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(quietHoursStart);
        LocalTime end = LocalTime.parse(quietHoursEnd);
        
        if (start.isBefore(end)) {
            return now.isAfter(start) && now.isBefore(end);
        } else {
            return now.isAfter(start) || now.isBefore(end);
        }
    }

    private LocalDateTime calculateNextDeliveryTime() {
        LocalTime end = LocalTime.parse(quietHoursEnd);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(end);
        
        if (next.isBefore(now)) {
            next = next.plusDays(1);
        }
        
        return next;
    }

    private String determinePriority(String eventType) {
        return switch (eventType) {
            case "OTP_LOGIN", "OTP_ORDER" -> "HIGH";
            case "ORDER_PLACED", "ORDER_ACCEPTED", "ORDER_DELIVERED" -> "NORMAL";
            default -> "LOW";
        };
    }

    private List<String> getProviderPriority() {
        return Arrays.asList(providerPriority.split(","));
    }

    /**
     * Process pending scheduled notifications
     */
    public void processScheduledNotifications() {
        try {
            List<NotificationEvent> scheduledEvents = eventRepository
                .findByStatusAndScheduledAtBefore("PENDING", LocalDateTime.now());
            
            for (NotificationEvent event : scheduledEvents) {
                processNotificationEvent(event);
            }
            
            logger.info("Processed {} scheduled notifications", scheduledEvents.size());
        } catch (Exception e) {
            logger.error("Error processing scheduled notifications: {}", e.getMessage(), e);
        }
    }
}