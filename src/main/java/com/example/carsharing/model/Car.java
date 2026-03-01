package com.example.carsharing.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

@Entity  // Говорит JPA, что это сущность для таблицы в БД
@Table(name = "cars")  // Имя таблицы в БД
@Data
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // AUTO_INCREMENT в БД
    private Long id;

    @Column(nullable = false, length = 50)  // NOT NULL, VARCHAR(50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "license_plate", nullable = false, unique = true, length = 10)
    private String licensePlate;

    @Column(nullable = false, length = 20)
    private String status;  // AVAILABLE, RENTED, SERVICE

    @Column(nullable = false)
    private Integer year;

    @Column(name = "price_per_hour", nullable = false)
    private Double pricePerHour;

    // Связь с арендами: одна машина может быть арендована много раз
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude  // Чтобы избежать зацикливания при выводе
    private List<Rental> rentals = new ArrayList<>();
}