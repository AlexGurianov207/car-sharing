package com.example.carsharing.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class TariffCreateRequest {

    @NotBlank(message = "Tariff name is required")
    @Size(min = 2, max = 50, message = "Tariff name must be between 2 and 50 characters")
    private String name;

    private String description;

    @NotNull(message = "Price per hour is required")
    @Positive(message = "Price per hour must be positive")
    private Double pricePerHour;

    @Positive(message = "Price per day must be positive")
    private Double pricePerDay;

    @Min(value = 1, message = "Minimum rental hours must be at least 1")
    private Integer minRentalHours;

    @Positive(message = "Maximum rental days must be positive")
    private Integer maxRentalDays;

    private Boolean isActive;
}