package com.example.carsharing.model;

import lombok.Data;

@Data
public class Car {
    private String licensePlate;
    private Long id;
    private String brand;
    private String model;
    private int year;
    private double pricePerHour;
    private String status;
}
