package com.agriconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================
 * MODEL - ReturnRequest
 * ============================================================
 *
 * Represents a freshness-quality return/refund request by a Consumer.
 * Used in Module 3: Freshness Guarantee / Returns (Minor feature).
 *
 * JPA: @ManyToOne → Each return request links to one PaymentTransaction.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "proof_image")
    private String proofImage;         // URL/path to uploaded proof image

    @Column(name = "request_time")
    private LocalDateTime requestTime = LocalDateTime.now();

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status")
    private ReturnStatus returnStatus = ReturnStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private PaymentTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    public enum ReturnStatus {
        PENDING, APPROVED, REJECTED
    }
}
