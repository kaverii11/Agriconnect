package com.agriconnect.pattern.strategy;

import com.agriconnect.model.HarvestBatch;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Strategy: Concrete Implementation 2
 * ============================================================
 *
 * ExpiryUrgencyStrategy — applies a time-based markdown.
 * When produce is close to expiry, the price is slashed to
 * move inventory quickly and reduce food waste.
 *
 * NOTE: This class demonstrates OCP — a BRAND NEW pricing
 *       algorithm added WITHOUT touching PricingStrategy interface
 *       or any existing code. The interface is extended, never modified.
 *
 * MARKDOWN TIERS:
 *   ≤ 1 day  expiry → 50% off
 *   ≤ 3 days expiry → 30% off
 *   ≤ 7 days expiry → 15% off
 */
@Component("expiryUrgencyStrategy")
public class ExpiryUrgencyStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculateDynamicPrice(HarvestBatch batch, double quantity) {
        long daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), batch.getExpiryDate());

        BigDecimal base = batch.getBasePrice();
        BigDecimal discountRate;

        if (daysToExpiry <= 1) {
            discountRate = BigDecimal.valueOf(0.50);
        } else if (daysToExpiry <= 3) {
            discountRate = BigDecimal.valueOf(0.30);
        } else if (daysToExpiry <= 7) {
            discountRate = BigDecimal.valueOf(0.15);
        } else {
            discountRate = BigDecimal.ZERO;
        }

        return base.subtract(base.multiply(discountRate))
                   .setScale(2, RoundingMode.HALF_UP);
    }
}
