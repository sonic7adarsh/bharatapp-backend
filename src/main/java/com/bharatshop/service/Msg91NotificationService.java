package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Msg91NotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(Msg91NotificationService.class);
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    private final String apiKey;
    private final String templateId;
    private final String senderId;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Msg91NotificationService(String apiKey, String templateId, String senderId) {
        this.apiKey = apiKey;
        this.templateId = templateId;
        this.senderId = senderId;
    }

    @Override
    public void sendSms(String phone, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("MSG91 not configured: missing API key");
            return;
        }
        if (phone == null || phone.isBlank()) {
            log.warn("MSG91 SMS not sent: phone missing");
            return;
        }
        String otp = extractOtp(message);
        if (otp == null) {
            log.warn("MSG91 SMS not sent: could not extract OTP from message");
            return;
        }
        try {
            String body = "{\"template_id\":\"" + templateId + "\",\"mobile\":\"" + phone + "\",\"otp\":\"" + otp + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.msg91.com/api/v5/otp"))
                    .header("Content-Type", "application/json")
                    .header("authkey", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.info("MSG91 OTP sent: phone={} status={}", phone, res.statusCode());
            } else {
                log.warn("MSG91 OTP send failed: phone={} status={} body={} ", phone, res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.error("MSG91 OTP send error: {}", e.getMessage());
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String message) {
        // Not supported here; rely on separate mail service or logging fallback
        log.info("[Email Fallback] to={} subject={} message={}", toEmail, subject, message);
    }

    private String extractOtp(String message) {
        if (message == null) return null;
        Matcher m = OTP_PATTERN.matcher(message);
        return m.find() ? m.group(1) : null;
    }
}