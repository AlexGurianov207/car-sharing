package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Extra service response")
public class ExtraServiceResponse {
    @Schema(description = "Service ID", example = "10")
    private Long id;

    @Schema(description = "Service name", example = "GPS")
    private String name;

    @Schema(description = "Service description", example = "Portable GPS navigator")
    private String description;

    @Schema(description = "Price per day", example = "5.0")
    private Double pricePerDay;

    @Schema(description = "Service category", example = "COMFORT")
    private String category;

    @Schema(description = "Active flag", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation date-time")
    private LocalDateTime createdAt;
}
