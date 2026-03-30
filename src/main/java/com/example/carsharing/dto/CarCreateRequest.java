package com.example.carsharing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CarCreateRequest {

    @NotBlank(message = "License plate is required")
    @Pattern(
            regexp = "^[A-Z0-9-]{4,12}$",
            message = "License plate must contain 4-12 uppercase letters, numbers or hyphen"
    )
    private String licensePlate;

    @NotBlank(message = "Brand is required")
    @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    private String model;

    @Min(value = 1980, message = "Year must be at least 1980")
    @Max(value = 2100, message = "Year must not exceed 2100")
    private int year;

    @Positive(message = "Price per hour must be positive")
    private double pricePerHour;
}
