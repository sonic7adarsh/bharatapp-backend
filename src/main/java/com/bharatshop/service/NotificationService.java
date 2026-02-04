package com.bharatshop.service;

import com.bharatshop.entity.NotificationEvent;
import com.bharatshop.entity.NotificationLog;
import com.bharatshop.entity.NotificationTemplate;
import com.bharatshop.entity.UserNotificationPreference;
import com.bharatshop.enums.NotificationEventType;
import com.bharatshop.domain.Order;
import com.bharatshop.domain.Store;
import org.springframework.scheduling.annotation.Async;
import com.bharatshop.repository.NotificationEventRepository;
import com.bharatshop.repository.NotificationLogRepository;
import com.bharatshop.repository.NotificationTemplateRepository;
import com.bharatshop.repository.UserNotificationPreferenceRepository;
import com.bharatshop.repository.RiderRepository;
import com.bharatshop.repository.OrderDeliveryRepository;
import com.bharatshop.repository.UserRepository;
import com.bharatshop.entity.RiderEntity;
import com.bharatshop.entity.OrderDeliveryEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

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
    private final StoreService storeService; // Injected
    private final RiderRepository riderRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final UserRepository userRepository;
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
            @Nullable EmailService emailService,
            StoreService storeService,
            RiderRepository riderRepository,
            OrderDeliveryRepository orderDeliveryRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.whatsAppService = whatsAppService;
        this.smsService = smsService;
        this.emailService = emailService;
        this.storeService = storeService;
        this.riderRepository = riderRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Send notification to user for a specific event
     */
    public CompletableFuture<Void> sendNotification(String userId, String eventType, Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            try {
                NotificationEvent event = createNotificationEvent(userId, eventType, data);
                processNotificationEvent(event);
            } catch (Exception e) {
                logger.error("Failed to send notification for event {} to user {}: {}", eventType, userId, e.getMessage(), e);
            }
        });
    }

    /**
     * Send OTP notification
     */
    public CompletableFuture<Void> sendOtp(String userId, String phone, String otp, String otpType) {
        Map<String, Object> data = new HashMap<>();
        data.put("otp", otp);
        data.put("otpType", otpType);
        data.put("phone", phone);
        
        String eventType = "OTP_" + otpType.toUpperCase();
        return sendNotification(userId, eventType, data);
    }

    // --- NEW EVENT SYSTEM ---
    
    @Async
    public void sendLifecycleEvent(NotificationEventType eventType, Order order, Map<String, Object> extraData) {
        logger.info("🔔 [TRIGGER] Processing Lifecycle Event: {}", eventType);
        try {
            switch (eventType) {
                case ORDER_PLACED -> {
                    notifyCustomer(order, "order_placed", List.of(order.getReference()), 
                        "Your order #" + order.getReference() + " has been placed.");
                    // notifySeller is handled by NEW_ORDER_RECEIVED event usually, but if simultaneous:
                }
                case NEW_ORDER_RECEIVED -> notifySeller(order, "seller_new_order", List.of(order.getReference()), 
                        "You have a new order #" + order.getReference());
                case ORDER_CONFIRMED -> notifyCustomer(order, "order_confirmed", List.of(order.getReference()), 
                        "Your order #" + order.getReference() + " is confirmed.");
                case ORDER_CANCELLED -> {
                    String reason = extraData != null ? (String) extraData.get("reason") : "Cancelled";
                    notifyCustomer(order, "order_cancelled", List.of(order.getReference(), reason), 
                        "Order #" + order.getReference() + " cancelled. Reason: " + reason);
                    notifySeller(order, "seller_order_cancelled", List.of(order.getReference(), reason), 
                        "Order #" + order.getReference() + " cancelled by " + reason);
                    notifyRider(order, "rider_order_cancelled", List.of(order.getReference()), 
                        "Trip cancelled for Order #" + order.getReference());
                }
                case ORDER_DELIVERED -> notifyCustomer(order, "order_delivered", List.of(order.getReference()), 
                        "Your order #" + order.getReference() + " has been delivered. Enjoy!");
                case DELIVERY_ASSIGNED -> notifyRider(order, "rider_delivery_assigned", List.of(order.getReference()), 
                        "New delivery assigned: #" + order.getReference());
                default -> logger.warn("⚠️ [TRIGGER] Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("❌ [TRIGGER] Failed to process event {}", eventType, e);
        }
    }

    @Async
    public void sendOtp(String phone, String otp) {
        logger.info("🔔 [TRIGGER] Sending OTP via WhatsApp to {}", phone);
        if (whatsappEnabled) {
            // Use sendOtp method which handles template fallback internally
            whatsAppService.sendOtp(phone, otp);
        }
    }

    private void notifyCustomer(Order order, String templateName, List<String> params, String fallbackMessage) {
        if (order.getUserId() != null) {
            userRepository.findById(order.getUserId()).ifPresent(user -> {
                if (whatsappEnabled && user.getPhone() != null) {
                    // FORCE TEXT MODE for MVP-1 (Templates not configured)
                    logger.info("📨 [ROUTING] Sending TEXT notification to Customer: {}", user.getPhone());
                    boolean sent = whatsAppService.sendMessage(user.getPhone(), fallbackMessage);
                    if (sent) logger.info("✅ [SENT] Text notification sent to Customer: {}", user.getPhone());
                    else logger.error("❌ [FAILED] Text notification failed for Customer: {}", user.getPhone());
                }
            });
        }
    }

    private void notifySeller(Order order, String templateName, List<String> params, String fallbackMessage) {
        if (order.getStoreId() != null) {
            storeService.getStoreById(order.getStoreId()).ifPresent(store -> {
                if (store.getOwnerPhone() != null && whatsappEnabled) {
                    // FORCE TEXT MODE for MVP-1 (Templates not configured)
                    logger.info("📨 [ROUTING] Sending TEXT notification to Seller: {}", store.getOwnerPhone());
                    boolean sent = whatsAppService.sendMessage(store.getOwnerPhone(), fallbackMessage);
                    if (sent) logger.info("✅ [SENT] Text notification sent to Seller: {}", store.getOwnerPhone());
                    else logger.error("❌ [FAILED] Text notification failed for Seller: {}", store.getOwnerPhone());
                }
            });
        }
    }

    private void notifyRider(Order order, String templateName, List<String> params, String fallbackMessage) {
        List<OrderDeliveryEntity> deliveries = orderDeliveryRepository.findByOrderId(order.getId());
        for (OrderDeliveryEntity delivery : deliveries) {
            if (delivery.getRiderId() != null) {
                riderRepository.findById(delivery.getRiderId()).ifPresent(rider -> {
                    if (whatsappEnabled && rider.getPhone() != null) {
                        // FORCE TEXT MODE for MVP-1 (Templates not configured)
                        logger.info("📨 [ROUTING] Sending TEXT notification to Rider: {}", rider.getPhone());
                        boolean sent = whatsAppService.sendMessage(rider.getPhone(), fallbackMessage);
                        if (sent) logger.info("✅ [SENT] Text notification sent to Rider: {}", rider.getPhone());
                        else logger.error("❌ [FAILED] Text notification failed for Rider: {}", rider.getPhone());
                    }
                });
            }
        }
    }

    /**
     * Send order notification
     */
    public CompletableFuture<Void> sendOrderNotification(String userId, String orderId, String orderStatus, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderStatus", orderStatus);
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        String eventType = "ORDER_" + orderStatus.toUpperCase();
        return sendNotification(userId, eventType, data);
    }

    /**
     * Send seller notification
     */
    public CompletableFuture<Void> sendSellerNotification(String sellerId, String eventType, Map<String, Object> data) {
        return sendNotification(sellerId, eventType, data);
    }

    private NotificationEvent createNotificationEvent(String userId, String eventType, Map<String, Object> data) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        // tenantId removed
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
            List<UserNotificationPreference> preferences = preferenceRepository.findByUserId(event.getUserId());
            
            if (preferences.isEmpty()) {
                // Create default preferences if none exist
                createDefaultPreferences(event.getUserId());
                preferences = preferenceRepository.findByUserId(event.getUserId());
            }
            
            // Filter preferences by enabled channels and quiet hours
            List<UserNotificationPreference> activePreferences = preferences.stream()
                .filter(pref -> pref.isEnabled() && !isQuietHoursForPreference(pref))
                .toList();
            
            if (activePreferences.isEmpty()) {
                logger.info("🚫 [SKIPPED] No active notification preferences for user {}", 
                    event.getUserId());
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
                    logger.info("🔄 [PROCESSING] Trying channel: {} for event: {}", channel, event.getEventType());
                    sent = sendNotificationThroughChannel(event, channel, preference.get().getLanguage());
                    if (sent) {
                        logger.info("✅ [SENT] Notification sent successfully via {}", channel);
                        break;
                    } else {
                        logger.warn("⚠️ [FAILED] Channel {} failed, trying next...", channel);
                    }
                }
            }
            
            // Update event status
            event.setStatus(sent ? "SENT" : "FAILED");
            event.setProcessedAt(LocalDateTime.now());
            eventRepository.save(event);
            
        } catch (Exception e) {
            logger.error("❌ [ERROR] Error processing notification event {}: {}", event.getId(), e.getMessage(), e);
            event.setStatus("ERROR");
            event.setProcessedAt(LocalDateTime.now());
            eventRepository.save(event);
        }
    }

    private boolean sendNotificationThroughChannel(NotificationEvent event, String channel, String language) {
        try {
            // Get template for this event and channel
            Optional<NotificationTemplate> templateOpt = templateRepository
                .findByEventTypeAndChannelAndLanguageAndIsActive(
                    event.getEventType(), channel, language, true);
            
            if (templateOpt.isEmpty()) {
                // Try with default language
                templateOpt = templateRepository
                    .findByEventTypeAndChannelAndLanguageAndIsActive(
                        event.getEventType(), channel, defaultLanguage, true);
            }
            
            if (templateOpt.isEmpty()) {
                logger.warn("📄 [TEMPLATE] No template found for event {} channel {} language {}. Falling back to text.", 
                    event.getEventType(), channel, language);
                
                // Fallback to simple text message
                String fallbackMessage = generateFallbackMessage(event);
                return sendSimpleMessage(event, channel, fallbackMessage);
            }
            
            NotificationTemplate template = templateOpt.get();
            logger.info("📄 [TEMPLATE] Found template: {}", template.getTemplateName());
            String message = processTemplate(template.getContent(), event.getEventData());
            String subject = template.getSubject() != null ? 
                processTemplate(template.getSubject(), event.getEventData()) : null;
            
            // Get recipient based on channel
            String recipient = getRecipientForChannel(event, channel);
            if (recipient == null) {
                logger.warn("⚠️ [RECIPIENT] No recipient found for user {} channel {}", event.getUserId(), channel);
                return false;
            }
            
            // Send through appropriate service
            boolean sent = switch (channel) {
                case "WHATSAPP" -> whatsappEnabled && whatsAppService.sendMessage(recipient, message);
                case "SMS" -> smsEnabled && smsService.sendMessage(recipient, message);
                case "EMAIL" -> emailEnabled && emailService != null && emailService.sendMessage(recipient, subject, message);
                default -> false;
            };
            
            // Log the notification attempt
            // Provider is typically stored in template, but if not available we can infer or use channel
            // Note: NotificationTemplate doesn't have provider field in my previous read, but I'll check if I need it.
            // The previous code used template.getProvider() which might have been a compilation error or I missed it.
            // Let's assume template has provider or we just use channel as provider for now if template.getProvider() is missing.
            // Looking at NotificationTemplate entity earlier, it does NOT have getProvider(). It has channel.
            // So previous code `template.getProvider()` was likely an error or I missed it.
            // I'll use channel as provider name for now to be safe.
            logNotification(event, channel, channel, recipient, message, sent, null);
            
            return sent;
            
        } catch (Exception e) {
            logger.error("❌ [ERROR] Error sending notification through channel {}: {}", channel, e.getMessage(), e);
            logNotification(event, channel, "UNKNOWN", null, null, false, e.getMessage());
            return false;
        }
    }

    private boolean sendSimpleMessage(NotificationEvent event, String channel, String message) {
        String recipient = getRecipientForChannel(event, channel);
        if (recipient == null) {
            logger.warn("⚠️ [RECIPIENT] No recipient found for user {} channel {} (fallback)", event.getUserId(), channel);
            return false;
        }

        logger.info("📨 [FALLBACK] Sending simple text via {}: {}", channel, message);
        boolean sent = switch (channel) {
            case "WHATSAPP" -> whatsappEnabled && whatsAppService.sendMessage(recipient, message);
            case "SMS" -> smsEnabled && smsService.sendMessage(recipient, message);
            case "EMAIL" -> emailEnabled && emailService != null && emailService.sendMessage(recipient, "Notification: " + event.getEventType(), message);
            default -> false;
        };
        
        if (sent) logger.info("✅ [SENT] Fallback notification sent via {}", channel);
        else logger.error("❌ [FAILED] Fallback notification failed via {}", channel);

        logNotification(event, channel, "FALLBACK", recipient, message, sent, null);
        return sent;
    }

    private String generateFallbackMessage(NotificationEvent event) {
        String type = event.getEventType();
        Map<String, Object> data = event.getEventData();
        
        if (type.contains("ORDER_PLACED") || type.contains("ORDER_placed")) {
            String ref = data.containsKey("reference") ? (String) data.get("reference") : 
                         data.containsKey("orderId") ? (String) data.get("orderId") : "";
            return "Your order " + ref + " has been placed successfully.";
        }
        if (type.contains("NEW_ORDER") || type.contains("new_order")) {
            String id = data.containsKey("orderId") ? (String) data.get("orderId") : "";
            return "You have a new order " + id + ". Please check your dashboard.";
        }
        if (type.contains("ORDER_CANCELLED")) {
            String ref = data.containsKey("reference") ? (String) data.get("reference") : 
                         data.containsKey("orderId") ? (String) data.get("orderId") : "";
            String reason = data.containsKey("reason") ? (String) data.get("reason") : "Cancelled";
            return "Order " + ref + " has been cancelled. Reason: " + reason;
        }
        
        // Generic fallback
        return "Notification: " + type + ". " + data.toString();
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

    private void createDefaultPreferences(String userId) {
        String[] channels = {"WHATSAPP", "SMS", "EMAIL"};
        for (String channel : channels) {
            UserNotificationPreference preference = new UserNotificationPreference();
            preference.setId(UUID.randomUUID().toString());
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
