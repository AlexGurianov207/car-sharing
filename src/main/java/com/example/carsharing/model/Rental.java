package com.example.carsharing.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rentals")
@Data
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
            name = "rental_selected_services",
            joinColumns = @JoinColumn(name = "rental_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @ToString.Exclude
    private List<ExtraService> selectedServices = new ArrayList<>();

    @OneToOne(mappedBy = "rental", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Payment payment;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    public Double calculateTotalPrice() {
        if (endTime == null) return null;

        long hours = Duration.between(startTime, endTime).toHours();
        if (hours < 1) hours = 1;

        double carPrice = car.getPricePerHour() * hours;

        long finalHours = hours;
        double servicesPrice = selectedServices.stream()
                .mapToDouble(service -> {
                    long days = (finalHours / 24) + (((finalHours % 24) == 0) ? 0 : 1);
                    return service.getPricePerDay() * days;
                })
                .sum();

        return carPrice + servicesPrice;
    }

    public PriceDetails getPriceDetails() {
        if (endTime == null) return null;

        long hours = Duration.between(startTime, endTime).toHours();
        if (hours < 1) hours = 1;
        long days = hours / 24 + (hours % 24 == 0 ? 0 : 1);

        double carPrice = car.getPricePerHour() * hours;
        double servicesPrice = selectedServices.stream()
                .mapToDouble(s -> s.getPricePerDay() * days)
                .sum();

        return new PriceDetails(carPrice, servicesPrice, carPrice + servicesPrice, hours, days);
    }

    @Data
    @AllArgsConstructor
    public static class PriceDetails {
        private double carAmount;
        private double servicesAmount;
        private double totalAmount;
        private long rentalHours;
        private long rentalDays;
    }
}