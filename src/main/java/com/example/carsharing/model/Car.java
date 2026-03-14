package com.example.carsharing.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cars")
@Data
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "license_plate", nullable = false, unique = true, length = 10)
    private String licensePlate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CarStatus status;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "price_per_hour", nullable = false)
    private Double pricePerHour;

    @OneToMany(mappedBy = "car")
    @ToString.Exclude
    private List<Rental> rentals = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "car_available_services",
            joinColumns = @JoinColumn(name = "car_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @ToString.Exclude
    private List<ExtraService> availableServices = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CarStatus.AVAILABLE;
        }
    }
}