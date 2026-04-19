package com.agriconnect.pattern.strategy;

import com.agriconnect.model.HarvestBatch;
import java.math.BigDecimal;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Strategy (Module 1)
 * ============================================================
 *
 * PATTERN: Strategy (Behavioral)
 *   Defines a FAMILY of algorithms (pricing strategies),
 *   encapsulates each one, and makes them interchangeable.
 *   The algorithm (how price is calculated) varies independently
 *   of the clients (FarmerService) that use it.
 *
 * DESIGN PRINCIPLE: Open/Closed Principle (OCP)
 *   - This interface is the ABSTRACTION that allows extension.
 *   - The system is OPEN for extension: add a new strategy
 *     (e.g., FlashSaleStrategy) by creating a new class that
 *     implements THIS interface — no existing code changes needed.
 *   - The system is CLOSED for modification: FarmerService
 *     never needs to be modified when new strategies are added.
 *
 * HOW TO ADD A NEW STRATEGY:
 *   1. Create: class FlashSaleStrategy implements PricingStrategy
 *   2. Override: calculateDynamicPrice()
 *   3. Inject it — zero changes to FarmerService. ✅ OCP satisfied.
 */
public interface PricingStrategy {

    /**
     * Calculates the dynamic (discounted) price for a harvest batch.
     *
     * @param batch    the harvest batch whose price should be computed
     * @param quantity the quantity the buyer is purchasing (may influence price)
     * @return         the final adjusted price per kg
     */
    BigDecimal calculateDynamicPrice(HarvestBatch batch, double quantity);
}
