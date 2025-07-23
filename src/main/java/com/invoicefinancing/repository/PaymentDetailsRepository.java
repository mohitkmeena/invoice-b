package com.invoicefinancing.repository;

import com.invoicefinancing.entity.PaymentDetails;
import com.invoicefinancing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    Optional<PaymentDetails> findByUser(User user);
    Optional<PaymentDetails> findByUserId(Long userId);
}