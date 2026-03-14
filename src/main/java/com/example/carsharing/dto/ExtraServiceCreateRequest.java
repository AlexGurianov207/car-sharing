package com.example.carsharing.dto;

import com.example.carsharing.model.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

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

    @NotNull(message = "Category is required")
    private ServiceCategory category;

    private Boolean isActive;
}