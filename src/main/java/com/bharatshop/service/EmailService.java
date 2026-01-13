package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${notifications.email.enabled:true}")
    private boolean enabled;
    
    @Value("${notifications.email.from-address:}")
    private String fromAddress;
    
    @Value("${notifications.email.from-name:BharatShop}")
    private String fromName;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public boolean sendMessage(String to, String subject, String text) {
        if (!enabled) {
            logger.info("Email notifications are disabled");
            return true; // Consider it successful if disabled
        }
        
        if (fromAddress == null || fromAddress.isEmpty()) {
            logger.warn("Email from address not configured");
            return false;
        }
        
        try {
            // Create a simple text email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            
            logger.info("Email sent successfully to {} with subject: {}", to, subject);
            return true;
            
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean sendHtmlMessage(String to, String subject, String htmlContent) {
        if (!enabled) {
            logger.info("Email notifications are disabled");
            return true; // Consider it successful if disabled
        }
        
        if (fromAddress == null || fromAddress.isEmpty()) {
            logger.warn("Email from address not configured");
            return false;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            logger.info("HTML email sent successfully to {} with subject: {}", to, subject);
            return true;
            
        } catch (Exception e) {
            logger.error("Error sending HTML email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}