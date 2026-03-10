package com.example.carsharing.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ExtraServiceCreateRequest {

    @NotBlank(message = "Service name is required")
    @Size(min = 2, max = 50, message = "Service name must be between 2 and 50 characters")
    private String name;

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @NotNull(message = "Price per day is required")
    @Positive(message = "Price must be positive")
    private Double pricePerDay;

    @NotBlank(message = "Category is required")
    private String category;  // SAFETY, COMFORT, EQUIPMENT, INSURANCE

    private Boolean isActive;
}