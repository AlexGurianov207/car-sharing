package com.example.carsharing.dto;

import lombok.Data;

import java.util.List;

@Data
public class CarResponse {
    private String licensePlate;
    private Long id;
    private String brand;
    private String model;
    private int year;
    private double pricePerHour;
    private String status;

    private List<CarServiceInfo> availableServices;

    @Data
    public static class CarServiceInfo {  // было ServiceInfo
        private Long id;
        private String name;
        private Double pricePerDay;
        private String category;
    }
}
