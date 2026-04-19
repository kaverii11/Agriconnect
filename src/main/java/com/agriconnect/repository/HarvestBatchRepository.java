package com.agriconnect.repository;

import com.agriconnect.model.HarvestBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for HarvestBatch.
 * Provides CRUD + custom query methods out-of-the-box.
 * DIP: Service layer depends on this interface (abstraction),
 * not on a concrete Hibernate DAO.
 */
@Repository
public interface HarvestBatchRepository extends JpaRepository<HarvestBatch, Long> {

    /** Find all batches belonging to a specific farmer. */
    List<HarvestBatch> findByFarmer_UserId(Long farmerId);

    /** Find active (non-expired, with remaining stock) batches by produce type. */
    @Query("SELECT h FROM HarvestBatch h WHERE h.produceType = :type " +
           "AND h.expiryDate >= CURRENT_DATE " +
           "AND (h.totalQuantity - h.quantitySold) > 0")
    List<HarvestBatch> findAvailableBatchesByType(@Param("type") String produceType);

    /** Farmer analytics: total quantity sold per batch (for dashboard). */
    @Query("SELECT h.produceType, SUM(h.quantitySold) FROM HarvestBatch h " +
           "WHERE h.farmer.userId = :farmerId GROUP BY h.produceType")
    List<Object[]> getSalesSummaryByFarmer(@Param("farmerId") Long farmerId);
}
