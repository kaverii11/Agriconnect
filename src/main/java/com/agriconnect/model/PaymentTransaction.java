package com.agriconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * PART 1: MODEL - PaymentTransaction
 * ============================================================
 *
 * DESIGN PRINCIPLE: SRP (Single Responsibility Principle)
 *   - PaymentTransaction has ONE responsibility: storing
 *     the financial record of a payment event.
 *   - It does NOT contain group-order pool logic.
 *   - This separation means changes to payment processing
 *     (e.g. adding GST) won't affect GroupOrder logic.
 *
 * JPA: @ManyToOne → Many transactions can reference one GroupOrder.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod;      // e.g. "UPI", "CARD"

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime = LocalDateTime.now();

    /**
     * SRP: Payment knows which order it belongs to,
     *      but GroupOrder does NOT handle payment logic.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private GroupOrder groupOrder;

    /** Who paid — the specific consumer. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
    }
}
