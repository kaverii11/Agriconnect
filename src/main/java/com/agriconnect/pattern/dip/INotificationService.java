package com.agriconnect.pattern.dip;

/**
 * ============================================================
 * PART 2: DESIGN PRINCIPLE - Dependency Inversion (DIP)
 * ============================================================
 *
 * PRINCIPLE: Dependency Inversion Principle (DIP)
 *   "High-level modules should not depend on low-level modules.
 *    Both should depend on ABSTRACTIONS. Abstractions should
 *    not depend on details." — Robert C. Martin
 *
 * THIS IS THE ABSTRACTION that the SubscriptionSchedulerService
 * (high-level) depends on — NOT on EmailNotificationService (low-level).
 *
 * This means:
 *   1. SubscriptionSchedulerService works with any service that
 *      implements INotificationService — testable with a mock.
 *   2. Swapping from email to SMS or push notification requires
 *      only a different @Primary bean — zero code change in the scheduler.
 *   3. The high-level "business logic" is insulated from the
 *      low-level "delivery mechanism" — DIP perfectly satisfied.
 */
public interface INotificationService {

    /**
     * Sends a renewal reminder notification to a user.
     *
     * @param recipientEmail the user's email address
     * @param subject        notification subject line
     * @param body           notification body text
     */
    void sendNotification(String recipientEmail, String subject, String body);

    /**
     * Sends a bulk notification to many users (e.g., all subscribers).
     *
     * @param emails  list of recipient email addresses
     * @param subject notification subject
     * @param body    notification body
     */
    void sendBulkNotification(java.util.List<String> emails, String subject, String body);
}
