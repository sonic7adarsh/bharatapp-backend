package com.bharatshop.config;

import com.bharatshop.service.LogNotificationService;
import com.bharatshop.service.Msg91NotificationService;
import com.bharatshop.service.NotificationService;
import com.bharatshop.service.TwilioNotificationService;
import com.bharatshop.service.SmtpEmailService;
import com.bharatshop.service.CompositeNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class NotificationConfig {
    private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    @Value("${notifications.provider:log}")
    private String provider;

    @Value("${notifications.msg91.apiKey:}")
    private String msg91ApiKey;

    @Value("${notifications.msg91.templateId:}")
    private String msg91TemplateId;

    @Value("${notifications.msg91.senderId:}")
    private String msg91SenderId;

    @Value("${notifications.twilio.accountSid:}")
    private String twilioAccountSid;

    @Value("${notifications.twilio.authToken:}")
    private String twilioAuthToken;

    @Value("${notifications.twilio.fromNumber:}")
    private String twilioFromNumber;

    @Value("${notifications.twilio.messagingServiceSid:}")
    private String twilioMessagingServiceSid;

    @Value("${notifications.email.from:${spring.mail.username:}}")
    private String emailFrom;

    @Bean
    public NotificationService notificationService(JavaMailSender mailSender) {
        NotificationService smsProvider;
        if ("msg91".equalsIgnoreCase(provider) && msg91ApiKey != null && !msg91ApiKey.isBlank()) {
            log.info("SMS provider: MSG91");
            smsProvider = new Msg91NotificationService(msg91ApiKey, msg91TemplateId, msg91SenderId);
        } else if ("twilio".equalsIgnoreCase(provider) && twilioAccountSid != null && !twilioAccountSid.isBlank() && twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            log.info("SMS provider: Twilio");
            smsProvider = new TwilioNotificationService(twilioAccountSid, twilioAuthToken, twilioFromNumber, twilioMessagingServiceSid);
        } else {
            log.info("SMS provider: LOG (fallback)");
            smsProvider = new LogNotificationService();
        }

        NotificationService emailProvider;
        if (emailFrom != null && !emailFrom.isBlank()) {
            log.info("Email provider: SMTP");
            emailProvider = new SmtpEmailService(mailSender, emailFrom);
        } else {
            log.info("Email provider: LOG (fallback)");
            emailProvider = new LogNotificationService();
        }

        return new CompositeNotificationService(smsProvider, emailProvider);
    }
}