package com.agriconnect.controller;

import com.agriconnect.model.Consumer;
import com.agriconnect.repository.ConsumerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * ============================================================
 * NEW: CONTROLLER LAYER - Wallet Management
 * ============================================================
 *
 * Provides endpoints for consumers to check and top-up their wallet balance.
 */
@RestController
@RequestMapping("/api/consumer/{consumerId}/wallet")
public class WalletController {

    private final ConsumerRepository consumerRepository;

    public WalletController(ConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    /**
     * Get the current wallet balance.
     */
    @GetMapping
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long consumerId) {
        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));
        return ResponseEntity.ok(consumer.getWalletBalance());
    }

    /**
     * Top-up the wallet balance.
     */
    @PostMapping("/topup")
    public ResponseEntity<BigDecimal> topUpBalance(@PathVariable Long consumerId, @RequestParam BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(BigDecimal.ZERO);
        }

        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));
        
        consumer.setWalletBalance(consumer.getWalletBalance().add(amount));
        consumerRepository.save(consumer);
        return ResponseEntity.ok(consumer.getWalletBalance());
    }
    
    /**
     * Mock an endpoint for sending insufficient balance notification email.
     */
    @PostMapping("/notify-insufficient")
    public ResponseEntity<String> notifyInsufficientBalance(@PathVariable Long consumerId) {
        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));
            
        // In a real application, INotificationService would be called here.
        System.out.println("EMAIL SENT: To " + consumer.getEmail() + " - ALERT: Insufficient Wallet Balance for upcoming subscription renewals.");
        
        return ResponseEntity.ok("Notification sent to " + consumer.getEmail());
    }
}
