package com.example.carsharing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "extra_services")
@Data
public class ExtraService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;  // "Детское кресло", "Навигатор", "Страховка"

    @Column(length = 200)
    private String description;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;  // цена за сутки использования

    @Column(nullable = false, length = 30)
    private String category;  // SAFETY, COMFORT, EQUIPMENT

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Связь с автомобилями, которые предлагают эту услугу
    @ManyToMany(mappedBy = "availableServices")
    @ToString.Exclude
    private List<Car> cars = new ArrayList<>();

    // Связь с арендами, которые выбрали эту услугу
    @ManyToMany(mappedBy = "selectedServices")
    @ToString.Exclude
    private List<Rental> rentals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}