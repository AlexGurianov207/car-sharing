package com.example.carsharing.dto;

import com.example.carsharing.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request for creating or updating extra service")
public class ExtraServiceCreateRequest {

    @Schema(description = "Service name", example = "GPS")
    @NotBlank(message = "Service name is required")
    @Size(min = 2, max = 50, message = "Service name must be between 2 and 50 characters")
    private String name;

    @Schema(description = "Service description", example = "Portable GPS navigator")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @Schema(description = "Price per day", example = "5.0")
    @NotNull(message = "Price per day is required")
    @Positive(message = "Price must be positive")
    private Double pricePerDay;

    @Schema(description = "Service category", example = "COMFORT")
    @NotNull(message = "Category is required")
    private ServiceCategory category;

    @Schema(description = "Active flag", example = "true")
    private Boolean isActive;
}
