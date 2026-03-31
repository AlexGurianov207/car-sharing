package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Car response")
public class CarResponse {
    @Schema(description = "License plate", example = "1234AB-7")
    private String licensePlate;

    @Schema(description = "Car ID", example = "1")
    private Long id;

    @Schema(description = "Brand", example = "Toyota")
    private String brand;

    @Schema(description = "Model", example = "Camry")
    private String model;

    @Schema(description = "Production year", example = "2020")
    private int year;

    @Schema(description = "Price per hour", example = "12.5")
    private double pricePerHour;

    @Schema(description = "Car status", example = "AVAILABLE")
    private String status;

    @Schema(description = "Available extra services")
    private List<CarServiceInfo> availableServices;

    @Data
    @Schema(description = "Short info about extra service available for car")
    public static class CarServiceInfo {
        @Schema(description = "Service ID", example = "10")
        private Long id;

        @Schema(description = "Service name", example = "GPS")
        private String name;

        @Schema(description = "Service price per day", example = "5.0")
        private Double pricePerDay;

        @Schema(description = "Service category", example = "COMFORT")
        private String category;
    }
}
