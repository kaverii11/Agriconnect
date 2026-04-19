package com.agriconnect.repository;

import com.agriconnect.model.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsumerRepository extends JpaRepository<Consumer, Long> {
    Optional<Consumer> findByEmail(String email);
}
