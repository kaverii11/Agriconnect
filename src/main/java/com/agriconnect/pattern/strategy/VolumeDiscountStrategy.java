package com.agriconnect.pattern.strategy;

import com.agriconnect.model.HarvestBatch;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Strategy: Concrete Implementation 1
 * ============================================================
 *
 * VolumeDiscountStrategy: the larger the purchase quantity,
 * the bigger the discount applied to the base price.
 *
 * DISCOUNT TIERS:
 *  ≥ 100 kg → 20% off base price
 *  ≥  50 kg → 10% off base price
 *  ≥  20 kg →  5% off base price
 *  <  20 kg →  no discount
 *
 * OCP SATISFACTION:
 *   Adding this class required NO modification to any existing
 *   class. FarmerService simply receives a PricingStrategy
 *   reference — it never knows (or cares) which concrete
 *   strategy is in use. The system is truly closed for modification.
 *
 * @Component — Spring manages this as a singleton bean so it can
 *              be injected anywhere via @Autowired.
 */
@Component("volumeDiscountStrategy")
public class VolumeDiscountStrategy implements PricingStrategy {

    private static final BigDecimal TIER_LARGE_THRESHOLD  = BigDecimal.valueOf(100);
    private static final BigDecimal TIER_MEDIUM_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal TIER_SMALL_THRESHOLD  = BigDecimal.valueOf(20);

    private static final BigDecimal DISCOUNT_LARGE  = BigDecimal.valueOf(0.20); // 20%
    private static final BigDecimal DISCOUNT_MEDIUM = BigDecimal.valueOf(0.10); // 10%
    private static final BigDecimal DISCOUNT_SMALL  = BigDecimal.valueOf(0.05); //  5%

    /**
     * Concrete Strategy method — implements the volume-discount algorithm.
     *
     * @param batch    the HarvestBatch — basePrice is read from it
     * @param quantity the purchase quantity in kg
     * @return         the discounted price per kg
     */
    @Override
    public BigDecimal calculateDynamicPrice(HarvestBatch batch, double quantity) {
        BigDecimal base        = batch.getBasePrice();
        BigDecimal quantityBd  = BigDecimal.valueOf(quantity);
        BigDecimal discountRate;

        // Determine discount tier
        if (quantityBd.compareTo(TIER_LARGE_THRESHOLD) >= 0) {
            discountRate = DISCOUNT_LARGE;
        } else if (quantityBd.compareTo(TIER_MEDIUM_THRESHOLD) >= 0) {
            discountRate = DISCOUNT_MEDIUM;
        } else if (quantityBd.compareTo(TIER_SMALL_THRESHOLD) >= 0) {
            discountRate = DISCOUNT_SMALL;
        } else {
            discountRate = BigDecimal.ZERO; // No discount for small quantities
        }

        // price = base * (1 - discountRate)
        BigDecimal discountAmount = base.multiply(discountRate);
        return base.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
