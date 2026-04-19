package com.agriconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * PART 1: MODEL - HarvestBatch
 * ============================================================
 *
 * Represents a batch of produce created by a Farmer.
 *
 * JPA ASSOCIATIONS:
 *   @ManyToOne  → HarvestBatch belongs to ONE Farmer
 *   @OneToMany  → HarvestBatch can have MANY GroupOrders
 *
 * OOP: Encapsulation — all fields are private.
 *      Business state (quantitySold) can only evolve through
 *      controlled methods like addQuantitySold().
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "harvest_batches")
public class HarvestBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long batchId;

    @NotBlank(message = "Produce type is required")
    @Column(name = "produce_type", nullable = false)
    private String produceType;         // e.g. "Organic Tomatoes"

    @Positive
    @Column(name = "total_quantity", nullable = false)
    private Double totalQuantity;       // in kg

    @Column(name = "quantity_sold")
    private Double quantitySold = 0.0;  // Tracks sold inventory

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Positive
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;       // Per kg base price

    /**
     * JPA: @ManyToOne — Many batches belong to one Farmer.
     * @JoinColumn defines the FK column in the harvest_batches table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    /**
     * JPA: @OneToMany — One batch can be part of many group orders.
     */
    @OneToMany(mappedBy = "harvestBatch", cascade = CascadeType.ALL)
    private List<GroupOrder> groupOrders = new ArrayList<>();

    // -------------------------------------------------------
    // ENCAPSULATION: Controlled business logic methods
    // -------------------------------------------------------

    /** Returns available stock remaining in this batch. */
    public Double getAvailableQuantity() {
        return totalQuantity - quantitySold;
    }

    /** Atomically record additional sold quantity. */
    public void addQuantitySold(double amount) {
        if (amount > getAvailableQuantity()) {
            throw new IllegalStateException("Insufficient stock in batch " + batchId);
        }
        this.quantitySold += amount;
    }

    /** Check if the batch has expired relative to today. */
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
}
