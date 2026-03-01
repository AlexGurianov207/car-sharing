package com.example.carsharing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tariffs")
@Data
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "price_per_hour", nullable = false)
    private Double pricePerHour;

    @Column(name = "price_per_day")
    private Double pricePerDay;

    @Column(name = "min_rental_hours", nullable = false)
    private Integer minRentalHours = 1;

    @Column(name = "max_rental_days")
    private Integer maxRentalDays;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (minRentalHours == null) {
            minRentalHours = 1;
        }
    }
}