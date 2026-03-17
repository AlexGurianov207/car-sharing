package com.example.carsharing.repository;

import com.example.carsharing.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByRentalId(Long rentalId);
}