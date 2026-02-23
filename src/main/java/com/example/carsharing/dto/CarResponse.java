package com.example.carsharing.dto;

import lombok.Data;

@Data
public class CarResponse {
    private String licensePlate;
    private Long id;
    private String brand;
    private String model;
    private int year;
    private double pricePerHour;
    private String status;
}
