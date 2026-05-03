package com.example.carsharing.repository;

import com.example.carsharing.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByRentalId(Long rentalId);

    List<Payment> findByRentalUserId(Long userId);

    Optional<Payment> findByIdAndRentalUserId(Long id, Long userId);
}
