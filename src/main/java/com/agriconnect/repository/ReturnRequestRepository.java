package com.agriconnect.repository;

import com.agriconnect.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByConsumer_UserId(Long consumerId);
    List<ReturnRequest> findByReturnStatus(ReturnRequest.ReturnStatus status);
}
