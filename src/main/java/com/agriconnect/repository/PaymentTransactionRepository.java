package com.agriconnect.repository;

import com.agriconnect.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByConsumer_UserId(Long consumerId);

    List<PaymentTransaction> findByGroupOrder_OrderId(Long orderId);
}
