package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request for creating or updating a car")
public class CarCreateRequest {

    @Schema(description = "Car license plate", example = "1234AB-7")
    @NotBlank(message = "License plate is required")
    @Pattern(
            regexp = "^[A-Z0-9-]{4,12}$",
            message = "License plate must contain 4-12 uppercase letters, numbers or hyphen"
    )
    private String licensePlate;

    @Schema(description = "Car brand", example = "Toyota")
    @NotBlank(message = "Brand is required")
    @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
    private String brand;

    @Schema(description = "Car model", example = "Camry")
    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    private String model;

    @Schema(description = "Production year", example = "2020")
    @Min(value = 1980, message = "Year must be at least 1980")
    @Max(value = 2100, message = "Year must not exceed 2100")
    private int year;

    @Schema(description = "Price per hour", example = "12.5")
    @Positive(message = "Price per hour must be positive")
    private double pricePerHour;
}
