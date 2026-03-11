package com.example.carsharing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "driver_license", nullable = false, unique = true, length = 20)
    private String driverLicense;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(nullable = false, length = 20)
    private String status;  // ACTIVE, BLOCKED, DELETED

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<Rental> rentals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }
}