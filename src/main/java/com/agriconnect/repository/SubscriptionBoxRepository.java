package com.agriconnect.repository;

import com.agriconnect.model.SubscriptionBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionBoxRepository extends JpaRepository<SubscriptionBox, Long> {

    List<SubscriptionBox> findByConsumer_UserId(Long consumerId);

    /** Find all active subscriptions due for delivery today (used by scheduler). */
    List<SubscriptionBox> findByActiveTrueAndNextDeliveryDate(LocalDate date);
}
