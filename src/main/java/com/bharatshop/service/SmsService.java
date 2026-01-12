package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    
    @Value("${notifications.sms.provider:msg91}")
    private String provider;
    
    @Value("${notifications.sms.msg91.api-url:}")
    private String msg91ApiUrl;
    
    @Value("${notifications.sms.msg91.auth-key:}")
    private String msg91AuthKey;
    
    @Value("${notifications.sms.msg91.sender-id:}")
    private String msg91SenderId;
    
    @Value("${notifications.sms.twilio.account-sid:}")
    private String twilioAccountSid;
    
    @Value("${notifications.sms.twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${notifications.sms.twilio.phone-number:}")
    private String twilioPhoneNumber;
    
    @Value("${notifications.sms.enabled:true}")
    private boolean enabled;
    
    private final RestTemplate restTemplate;
    
    public SmsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public boolean sendMessage(String phoneNumber, String message) {
        if (!enabled) {
            logger.info("SMS notifications are disabled");
            return true; // Consider it successful if disabled
        }
        
        return switch (provider.toLowerCase()) {
            case "msg91" -> sendViaMsg91(phoneNumber, message);
            case "twilio" -> sendViaTwilio(phoneNumber, message);
            default -> {
                logger.warn("Unknown SMS provider: {}", provider);
                yield false;
            }
        };
    }
    
    private boolean sendViaMsg91(String phoneNumber, String message) {
        if (msg91ApiUrl == null || msg91ApiUrl.isEmpty() || msg91AuthKey == null || msg91AuthKey.isEmpty()) {
            logger.warn("MSG91 API not configured properly");
            return false;
        }
        
        try {
            // Clean phone number
            String cleanPhone = phoneNumber.replaceAll("[^0-9+]", "");
            if (!cleanPhone.startsWith("+")) {
                cleanPhone = "91" + cleanPhone; // Remove + for MSG91
            } else {
                cleanPhone = cleanPhone.substring(1); // Remove +
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", msg91SenderId);
            payload.put("route", "4"); // Transactional route
            payload.put("country", "91");
            payload.put("sms", new Object[]{
                Map.of("message", message, "to", new String[]{cleanPhone})
            });
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", msg91AuthKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                msg91ApiUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("type"))) {
                    logger.info("MSG91 SMS sent successfully to {}", cleanPhone);
                    return true;
                } else {
                    logger.error("MSG91 API returned error: {}", responseBody);
                    return false;
                }
            } else {
                logger.error("Failed to send MSG91 SMS. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error sending MSG91 SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
    
    private boolean sendViaTwilio(String phoneNumber, String message) {
        if (twilioAccountSid == null || twilioAccountSid.isEmpty() || 
            twilioAuthToken == null || twilioAuthToken.isEmpty() ||
            twilioPhoneNumber == null || twilioPhoneNumber.isEmpty()) {
            logger.warn("Twilio API not configured properly");
            return false;
        }
        
        try {
            // Clean phone number
            String cleanPhone = phoneNumber.replaceAll("[^0-9+]", "");
            if (!cleanPhone.startsWith("+")) {
                cleanPhone = "+91" + cleanPhone; // Default to India country code
            }
            
            String twilioApiUrl = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";
            
            Map<String, String> payload = new HashMap<>();
            payload.put("From", twilioPhoneNumber);
            payload.put("To", cleanPhone);
            payload.put("Body", message);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(twilioAccountSid, twilioAuthToken);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                twilioApiUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("Twilio SMS sent successfully to {}", cleanPhone);
                return true;
            } else {
                logger.error("Failed to send Twilio SMS. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error sending Twilio SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}