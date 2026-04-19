package com.agriconnect.repository;

import com.agriconnect.model.GroupOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {

    /** All open group orders for a specific harvest batch. */
    List<GroupOrder> findByHarvestBatch_BatchIdAndStatus(Long batchId, GroupOrder.OrderStatus status);

    /** All orders a consumer participates in. */
    @Query("SELECT g FROM GroupOrder g JOIN g.participants p WHERE p.userId = :consumerId")
    List<GroupOrder> findOrdersByConsumer(@Param("consumerId") Long consumerId);

    /** Count of confirmed orders per farmer (analytics). */
    @Query("SELECT COUNT(g) FROM GroupOrder g WHERE g.harvestBatch.farmer.userId = :farmerId " +
           "AND g.status = 'CONFIRMED'")
    Long countConfirmedOrdersByFarmer(@Param("farmerId") Long farmerId);
}
