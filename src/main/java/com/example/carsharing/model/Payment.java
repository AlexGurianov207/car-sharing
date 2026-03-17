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
import jakarta.persistence.ManyToOne;
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
        if (status == null) {
            status = PaymentStatus.COMPLETED;
        }

        if (rental != null) {
            if (rental.getUser() != null) {
                this.userFullNameSnapshot =
                        (rental.getUser().getFirstName() != null ? rental.getUser().getFirstName() : "") + " " +
                                (rental.getUser().getLastName() != null ? rental.getUser().getLastName() : "");
                this.userFullNameSnapshot = this.userFullNameSnapshot.trim();
            }

            if (rental.getCar() != null) {
                this.carInfoSnapshot =
                        (rental.getCar().getBrand() != null ? rental.getCar().getBrand() : "") + " " +
                                (rental.getCar().getModel() != null ? rental.getCar().getModel() : "") +
                                (rental.getCar().getLicensePlate() != null ? " (" + rental.getCar().getLicensePlate() + ")" : "");
                this.carInfoSnapshot = this.carInfoSnapshot.trim();
            }

            this.rentalStartTimeSnapshot = rental.getStartTime();
            this.rentalEndTimeSnapshot = rental.getEndTime();

            if (rental.getStartTime() != null && rental.getEndTime() != null) {
                long hours = Duration.between(rental.getStartTime(), rental.getEndTime()).toHours();
                this.rentalHoursSnapshot = hours < 1 ? 1 : hours;
            }

            if (rental.getSelectedServices() != null && !rental.getSelectedServices().isEmpty()) {
                this.selectedServicesSnapshot = rental.getSelectedServices().stream()
                        .map(ExtraService::getName)
                        .collect(Collectors.joining(", "));
            }
        }
    }
}