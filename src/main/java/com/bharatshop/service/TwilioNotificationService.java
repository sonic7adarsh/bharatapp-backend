package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TwilioNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(TwilioNotificationService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber; // optional if using Messaging Service
    private final String messagingServiceSid; // optional alternative to fromNumber
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TwilioNotificationService(String accountSid, String authToken, String fromNumber, String messagingServiceSid) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.messagingServiceSid = messagingServiceSid;
    }

    @Override
    public void sendSms(String phone, String message) {
        if (isBlank(accountSid) || isBlank(authToken)) {
            log.warn("Twilio not configured: missing Account SID or Auth Token");
            return;
        }
        if (isBlank(phone)) {
            log.warn("Twilio SMS not sent: phone missing");
            return;
        }
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            StringBuilder form = new StringBuilder();
            form.append("To=").append(URLEncoder.encode(phone, StandardCharsets.UTF_8));
            form.append("&Body=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));
            if (!isBlank(messagingServiceSid)) {
                form.append("&MessagingServiceSid=").append(URLEncoder.encode(messagingServiceSid, StandardCharsets.UTF_8));
            } else if (!isBlank(fromNumber)) {
                form.append("&From=").append(URLEncoder.encode(fromNumber, StandardCharsets.UTF_8));
            } else {
                log.warn("Twilio SMS not sent: provide either MessagingServiceSid or From number");
                return;
            }

            String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.info("Twilio SMS sent: phone={} status={}", phone, res.statusCode());
            } else {
                log.warn("Twilio SMS failed: phone={} status={} body={}", phone, res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.error("Twilio SMS error: {}", e.getMessage());
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String message) {
        // Email is not handled by Twilio SMS provider; log as fallback
        log.info("[Email Fallback] to={} subject={} message={}", toEmail, subject, message);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}