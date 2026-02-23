package com.example.carsharing.dto;

import lombok.Data;

@Data
public class CarCreateRequest {
    private String licensePlate;
    private String brand;
    private String model;
    private int year;
    private double pricePerHour;
}
