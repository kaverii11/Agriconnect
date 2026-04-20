package com.agriconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_boxes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "box_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class SubscriptionBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "frequency", nullable = false)
    private String frequency;          // "WEEKLY", "BIWEEKLY", "MONTHLY"

    @Column(name = "next_delivery_date")
    private LocalDate nextDeliveryDate;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "price_per_cycle")
    private Double pricePerCycle;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    /** Factory method — subclasses define what items the box contains. */
    public abstract String getBoxDescription();

    /** Advance the next delivery date by one cycle. */
    public void scheduleNextDelivery() {
        if ("WEEKLY".equals(frequency)) {
            nextDeliveryDate = nextDeliveryDate.plusWeeks(1);
        } else if ("BIWEEKLY".equals(frequency)) {
            nextDeliveryDate = nextDeliveryDate.plusWeeks(2);
        } else {
            nextDeliveryDate = nextDeliveryDate.plusMonths(1);
        }
    }
}
