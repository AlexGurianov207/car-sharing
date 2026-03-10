package com.example.carsharing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double amount;  // Общая сумма

    @Column(name = "car_amount", nullable = false)
    private Double carAmount;  // Сумма за машину

    @Column(name = "services_amount", nullable = false)
    private Double servicesAmount;  // Сумма за услуги

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;  // CARD, CASH, APPLE_PAY, GOOGLE_PAY

    @Column(nullable = false, length = 20)
    private String status;  // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @PrePersist
    protected void onCreate() {
        paymentDate = LocalDateTime.now();
        if (status == null) {
            status = "COMPLETED";
        }
    }
}