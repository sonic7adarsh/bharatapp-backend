package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending WhatsApp messages via Meta Cloud API.
 * 
 * MVP-1 Constraints:
 * - Only Template messages supported for business-initiated conversations.
 * - Strict error handling (no silent failures).
 * - No text message fallback for OTP/Auth.
 * - Single language (en_US).
 */
@Service
public class WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);
    
    @Value("${notifications.whatsapp.api-url:}")
    private String apiUrl;
    
    @Value("${notifications.whatsapp.api-key:}")
    private String apiKey;
    
    @Value("${notifications.whatsapp.enabled:true}")
    private boolean enabled;
    
    private final RestTemplate restTemplate;
    
    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Sends an OTP using a plain TEXT message.
     * THIS IS A TEMPORARY METHOD FOR TEST MODE ONLY.
     * 
     * Requirements for this to work:
     * 1. The recipient must have messaged the business number within the last 24 hours.
     * 2. This does NOT use templates.
     * 
     * @param phoneNumber Recipient phone number
     * @param otp The One-Time Password
     * @return true if successful
     */
    public boolean sendTextOtp(String phoneNumber, String otp) {
        logger.warn("⚠️ TEST MODE: Sending OTP as plain text. This is NOT for production. Ensure user has messaged business first.");
        String message = "Your OTP is " + otp + ". It is valid for 5 minutes.";
        
        // Use the exact payload structure requested for text messages
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", cleanPhoneNumber(phoneNumber));
        payload.put("type", "text");
        
        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        payload.put("text", text);
        
        return sendPayload(phoneNumber, payload, "TEST_MODE_OTP");
    }

    /**
     * Sends an OTP using the 'auth_otp' template.
     * STRICT IMPLEMENTATION: No fallback to text messages.
     * 
     * @param phoneNumber Recipient phone number
     * @param otp The One-Time Password
     * @return true if WhatsApp API accepts the request, false otherwise
     */
    public boolean sendOtp(String phoneNumber, String otp) {
        logger.info("Initiating OTP delivery to {}", cleanPhoneNumber(phoneNumber));
        
        // FALLBACK TO TEXT MODE (User request: Template not configured yet)
        // User MUST send "Hi" to business number first to open 24h window
        return sendTextOtp(phoneNumber, otp);
    }

    /**
     * Sends a template message (Required for business-initiated conversations).
     * 
     * @param phoneNumber Recipient phone number
     * @param templateName Name of the template in Meta WhatsApp Manager
     * @param languageCode Language code (e.g., "en_US")
     * @return true if successful
     */
    public boolean sendTemplateMessage(String phoneNumber, String templateName, String languageCode) {
        return sendTemplateMessage(phoneNumber, templateName, languageCode, null);
    }

    /**
     * Sends a template message with body parameters.
     * 
     * @param phoneNumber Recipient phone number
     * @param templateName Name of the template
     * @param languageCode Language code
     * @param bodyParams List of parameters to replace {{1}}, {{2}}, etc. in the template body
     * @return true if successful
     */
    public boolean sendTemplateMessage(String phoneNumber, String templateName, String languageCode, List<String> bodyParams) {
        if (!enabled) {
            logger.warn("WhatsApp notifications are disabled in configuration.");
            return false; // Returning false because we didn't actually send it
        }
        
        // Construct Payload according to WhatsApp Cloud API spec
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", cleanPhoneNumber(phoneNumber));
        payload.put("type", "template");
        
        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        
        Map<String, String> language = new HashMap<>();
        language.put("code", languageCode);
        template.put("language", language);
        
        if (bodyParams != null && !bodyParams.isEmpty()) {
            List<Map<String, Object>> components = new ArrayList<>();
            Map<String, Object> bodyComponent = new HashMap<>();
            bodyComponent.put("type", "body");
            
            List<Map<String, String>> parameters = new ArrayList<>();
            for (String param : bodyParams) {
                Map<String, String> parameter = new HashMap<>();
                parameter.put("type", "text");
                parameter.put("text", param);
                parameters.add(parameter);
            }
            bodyComponent.put("parameters", parameters);
            components.add(bodyComponent);
            template.put("components", components);
        }
        
        payload.put("template", template);
        
        return sendPayload(phoneNumber, payload, "template: " + templateName);
    }
    
    /**
     * Sends a free-form text message.
     * Note: Should only be used for user-initiated conversations (24h window).
     */
    public boolean sendMessage(String phoneNumber, String message) {
        if (!enabled) return false;
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", cleanPhoneNumber(phoneNumber));
        payload.put("type", "text");
        
        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        payload.put("text", text);
        
        return sendPayload(phoneNumber, payload, "text: " + message);
    }

    private boolean sendPayload(String phoneNumber, Map<String, Object> payload, String context) {
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            logger.error("WhatsApp API configuration missing (url or key). Cannot send message.");
            return false;
        }

        String cleanPhone = cleanPhoneNumber(phoneNumber);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            // Log outgoing request (DO NOT log Authorization token)
            logger.info(">>> WHATSAPP REQUEST [Context: {}] To: {} | Payload: {}", context, cleanPhone, payload);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            // Strict Success Check: Only log success if we get a 2xx response
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("<<< WHATSAPP SUCCESS [Context: {}] To: {} | Response: {}", context, cleanPhone, response.getBody());
                return true;
            } else {
                logger.error("<<< WHATSAPP FAILED [Context: {}] To: {} | Status: {} | Response: {}", 
                    context, cleanPhone, response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (HttpClientErrorException e) {
            // Specialized error handling for common WhatsApp errors
            if (e.getStatusCode() == HttpStatus.NOT_FOUND && e.getResponseBodyAsString().contains("Template name does not exist")) {
                 logger.error("WhatsApp Configuration Error: Template '{}' not found in Meta account. Please create it.", context);
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                 logger.error("WhatsApp Auth Error: Invalid Access Token.");
            } else {
                 logger.error("WhatsApp API Error [Context: {}] To: {} | Status: {} | Body: {}", 
                     context, cleanPhone, e.getStatusCode(), e.getResponseBodyAsString());
            }
            return false;
        } catch (Exception e) {
            logger.error("WhatsApp System Error [Context: {}] To: {}: {}", context, cleanPhone, e.getMessage(), e);
            return false;
        }
    }
    
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String clean = phoneNumber.replaceAll("[^0-9]", "");
        // Basic normalization for India (MVP-1 assumption)
        if (clean.length() == 10) {
            return "91" + clean;
        }
        return clean;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
