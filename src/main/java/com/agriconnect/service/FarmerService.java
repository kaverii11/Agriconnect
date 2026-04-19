package com.agriconnect.service;

import com.agriconnect.model.Farmer;
import com.agriconnect.model.HarvestBatch;
import com.agriconnect.pattern.strategy.PricingStrategy;
import com.agriconnect.repository.FarmerRepository;
import com.agriconnect.repository.HarvestBatchRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * SERVICE LAYER - Module 1: Farmer & Inventory Management
 * ============================================================
 *
 * MVC ROLE: This is the MODEL/BUSINESS LOGIC layer.
 *   - FarmerController delegates all business logic HERE.
 *   - This service knows nothing about HTTP (no @RequestMapping).
 *   - This service knows nothing about the View (no model.addAttribute).
 *   This clean separation is what makes it strict MVC.
 *
 * DESIGN PATTERN: STRATEGY (used here as Context)
 *   FarmerService holds a reference to PricingStrategy (the interface).
 *   It does NOT know which concrete strategy is injected at runtime.
 *   The strategy can be swapped via Spring's @Qualifier or configuration.
 *
 * DESIGN PRINCIPLE: OCP
 *   FarmerService is CLOSED for modification.
 *   New pricing algorithms (FlashSaleStrategy, etc.) can be added by
 *   implementing PricingStrategy and changing the @Qualifier — zero
 *   change to THIS class's code.
 */
@Service
@Transactional
public class FarmerService {

    private final HarvestBatchRepository harvestBatchRepository;
    private final FarmerRepository       farmerRepository;

    /**
     * STRATEGY PATTERN — Context injects the Strategy.
     * @Qualifier selects which PricingStrategy bean to inject.
     * Swap "volumeDiscountStrategy" → "expiryUrgencyStrategy"
     * to change pricing logic without touching this class (OCP ✅).
     */
    private final PricingStrategy pricingStrategy;

    public FarmerService(HarvestBatchRepository harvestBatchRepository,
                         FarmerRepository farmerRepository,
                         @Qualifier("volumeDiscountStrategy") PricingStrategy pricingStrategy) {
        this.harvestBatchRepository = harvestBatchRepository;
        this.farmerRepository       = farmerRepository;
        this.pricingStrategy        = pricingStrategy;
    }

    // -------------------------------------------------------
    // HARVEST BATCH CRUD (Major Feature — Module 1)
    // -------------------------------------------------------

    /** CREATE a new harvest batch for a farmer. */
    public HarvestBatch createHarvestBatch(Long farmerId, HarvestBatch batch) {
        Farmer farmer = farmerRepository.findById(farmerId)
            .orElseThrow(() -> new IllegalArgumentException("Farmer not found: " + farmerId));
        batch.setFarmer(farmer);
        return harvestBatchRepository.save(batch);
    }

    /** READ all harvest batches for a farmer. */
    @Transactional(readOnly = true)
    public List<HarvestBatch> getBatchesByFarmer(Long farmerId) {
        return harvestBatchRepository.findByFarmer_UserId(farmerId);
    }

    /** READ a single batch by ID. */
    @Transactional(readOnly = true)
    public HarvestBatch getBatchById(Long batchId) {
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        return harvestBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("HarvestBatch not found: " + batchId));
    }

    /** UPDATE batch details (e.g., correct the expiry date). */
    public HarvestBatch updateBatch(Long batchId, HarvestBatch updatedBatch) {
        HarvestBatch existing = getBatchById(batchId);
        existing.setProduceType(updatedBatch.getProduceType());
        existing.setTotalQuantity(updatedBatch.getTotalQuantity());
        existing.setBasePrice(updatedBatch.getBasePrice());
        existing.setHarvestDate(updatedBatch.getHarvestDate());
        existing.setExpiryDate(updatedBatch.getExpiryDate());
        return harvestBatchRepository.save(existing);
    }

    /** DELETE a batch (only if no confirmed orders exist on it). */
    public void deleteBatch(Long batchId) {
        HarvestBatch batch = getBatchById(batchId);
        boolean hasActiveOrders = batch.getGroupOrders().stream()
            .anyMatch(o -> o.getStatus() == com.agriconnect.model.GroupOrder.OrderStatus.CONFIRMED
                        || o.getStatus() == com.agriconnect.model.GroupOrder.OrderStatus.OPEN);
        if (hasActiveOrders) {
            throw new IllegalStateException("Cannot delete batch with active group orders.");
        }
        harvestBatchRepository.delete(batch);
    }

    // -------------------------------------------------------
    // VOLUME-BASED DYNAMIC PRICING (Minor Feature — Module 1)
    // -------------------------------------------------------

    public BigDecimal calculatePrice(Long batchId, double quantity) {
        HarvestBatch batch = getBatchById(batchId);
        // Delegate entirely to the strategy — Context calls Strategy ✅
        return pricingStrategy.calculateDynamicPrice(batch, quantity);
    }

    // -------------------------------------------------------
    // FARMER ANALYTICS DASHBOARD (Minor Feature — Module 4)
    // -------------------------------------------------------

    /**
     * Returns a summary map of produce type → total kg sold.
     * Used by the Farmer Analytics Dashboard endpoint.
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getSalesSummary(Long farmerId) {
        List<Object[]> raw = harvestBatchRepository.getSalesSummaryByFarmer(farmerId);
        Map<String, Double> summary = new java.util.LinkedHashMap<>();
        for (Object[] row : raw) {
            if (row != null && row.length >= 2) {
                summary.put((String) row[0], (Double) row[1]);
            }
        }
        return summary;
    }
}
