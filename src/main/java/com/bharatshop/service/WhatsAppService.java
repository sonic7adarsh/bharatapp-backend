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
public class WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);
    
    @Value("${notifications.whatsapp.api-url:}")
    private String apiUrl;
    
    @Value("${notifications.whatsapp.api-key:}")
    private String apiKey;
    
    @Value("${notifications.whatsapp.sender-number:}")
    private String senderNumber;
    
    @Value("${notifications.whatsapp.enabled:true}")
    private boolean enabled;
    
    private final RestTemplate restTemplate;
    
    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public boolean sendMessage(String phoneNumber, String message) {
        if (!enabled) {
            logger.info("WhatsApp notifications are disabled");
            return true; // Consider it successful if disabled
        }
        
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            logger.warn("WhatsApp API not configured properly");
            return false;
        }
        
        try {
            // Clean phone number
            String cleanPhone = phoneNumber.replaceAll("[^0-9+]", "");
            if (!cleanPhone.startsWith("+")) {
                cleanPhone = "+91" + cleanPhone; // Default to India country code
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("phone", cleanPhone);
            payload.put("message", message);
            payload.put("sender", senderNumber);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("WhatsApp message sent successfully to {}", cleanPhone);
                return true;
            } else {
                logger.error("Failed to send WhatsApp message. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error sending WhatsApp message to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}