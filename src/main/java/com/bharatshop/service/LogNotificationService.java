package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void sendSms(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            log.warn("SMS not sent: phone missing");
            return;
        }
        log.info("[SMS] to={} message={}", phone, message);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String message) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Email not sent: recipient missing");
            return;
        }
        log.info("[Email] to={} subject={} message={}", toEmail, subject, message);
    }
}