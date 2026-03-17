package com.example.carsharing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", unique = true)
    private Rental rental;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "car_amount", nullable = false)
    private Double carAmount;

    @Column(name = "services_amount", nullable = false)
    private Double servicesAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "user_full_name_snapshot")
    private String userFullNameSnapshot;

    @Column(name = "car_info_snapshot")
    private String carInfoSnapshot;

    @Column(name = "rental_start_time_snapshot")
    private LocalDateTime rentalStartTimeSnapshot;

    @Column(name = "rental_end_time_snapshot")
    private LocalDateTime rentalEndTimeSnapshot;

    @Column(name = "rental_hours_snapshot")
    private Long rentalHoursSnapshot;

    @Column(name = "selected_services_snapshot")
    private String selectedServicesSnapshot;

    @PrePersist
    protected void onCreate() {
        paymentDate = LocalDateTime.now();
        setDefaultStatusIfNull();

        if (rental != null) {
            setUserFullNameSnapshot();
            setCarInfoSnapshot();
            setRentalTimeSnapshots();
            setRentalHoursSnapshot();
            setSelectedServicesSnapshot();
        }
    }

    private void setDefaultStatusIfNull() {
        if (status == null) {
            status = PaymentStatus.COMPLETED;
        }
    }

    private void setUserFullNameSnapshot() {
        if (rental.getUser() != null) {
            String firstName = getValueOrDefault(rental.getUser().getFirstName());
            String lastName = getValueOrDefault(rental.getUser().getLastName());
            this.userFullNameSnapshot = (firstName + " " + lastName).trim();
        }
    }

    private void setCarInfoSnapshot() {
        if (rental.getCar() != null) {
            String brand = getValueOrDefault(rental.getCar().getBrand());
            String model = getValueOrDefault(rental.getCar().getModel());
            String licensePlate = rental.getCar().getLicensePlate();

            this.carInfoSnapshot = brand + " " + model;
            if (licensePlate != null) {
                this.carInfoSnapshot += " (" + licensePlate + ")";
            }
            this.carInfoSnapshot = this.carInfoSnapshot.trim();
        }
    }

    private void setRentalTimeSnapshots() {
        this.rentalStartTimeSnapshot = rental.getStartTime();
        this.rentalEndTimeSnapshot = rental.getEndTime();
    }

    private void setRentalHoursSnapshot() {
        if (rental.getStartTime() != null && rental.getEndTime() != null) {
            long hours = Duration.between(rental.getStartTime(), rental.getEndTime()).toHours();
            this.rentalHoursSnapshot = Math.max(1, hours);
        }
    }

    private void setSelectedServicesSnapshot() {
        if (rental.getSelectedServices() != null && !rental.getSelectedServices().isEmpty()) {
            this.selectedServicesSnapshot = rental.getSelectedServices().stream()
                    .map(ExtraService::getName)
                    .collect(Collectors.joining(", "));
        }
    }

    private String getValueOrDefault(String value) {
        return value != null ? value : "";
    }
}