package com.agriconnect.pattern.dip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ============================================================
 * PART 2: DIP - Concrete Low-Level Module (Email Implementation)
 * ============================================================
 *
 * EmailNotificationService is the LOW-LEVEL concrete implementation.
 * The HIGH-LEVEL SubscriptionSchedulerService NEVER references this
 * concrete class directly — it only knows about INotificationService.
 *
 * DIP FLOW:
 *   SubscriptionSchedulerService
 *       ↑ depends on (interface)
 *   INotificationService
 *       ↑ implemented by
 *   EmailNotificationService  (this class)
 *
 * @Primary marks this as the default implementation when Spring
 * autowires INotificationService. To switch to SMS:
 *   1. Create SmsNotificationService implements INotificationService
 *   2. Mark it @Primary
 *   3. SubscriptionSchedulerService needs ZERO changes. ✅ DIP.
 */
@Service
@Primary
public class EmailNotificationService implements INotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public void sendNotification(String recipientEmail, String subject, String body) {
        // In production: integrate with JavaMailSender / SendGrid / AWS SES
        log.info("[EMAIL] To: {} | Subject: {} | Body: {}", recipientEmail, subject, body);
    }

    @Override
    public void sendBulkNotification(List<String> emails, String subject, String body) {
        log.info("[EMAIL BULK] Sending to {} recipients | Subject: {}", emails.size(), subject);
        emails.forEach(email -> sendNotification(email, subject, body));
    }
}
