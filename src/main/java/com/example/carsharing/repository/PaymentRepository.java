package com.example.carsharing.repository;

import com.example.carsharing.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    Optional<Payment> findByRentalId(Long rentalId);

    boolean existsByRentalId(Long rentalId);
}