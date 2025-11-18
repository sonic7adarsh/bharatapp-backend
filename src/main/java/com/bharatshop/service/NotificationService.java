package com.bharatshop.service;

public interface NotificationService {
    void sendSms(String phone, String message);
    void sendEmail(String toEmail, String subject, String message);
}