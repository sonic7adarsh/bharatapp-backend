package com.bharatshop.service;

public class CompositeNotificationService implements NotificationService {
    private final NotificationService smsProvider;
    private final NotificationService emailProvider;

    public CompositeNotificationService(NotificationService smsProvider, NotificationService emailProvider) {
        this.smsProvider = smsProvider;
        this.emailProvider = emailProvider;
    }

    @Override
    public void sendSms(String phone, String message) {
        if (smsProvider != null) {
            smsProvider.sendSms(phone, message);
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String message) {
        if (emailProvider != null) {
            emailProvider.sendEmail(toEmail, subject, message);
        }
    }
}