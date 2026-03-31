package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Rental response")
public class RentalResponse {
    @Schema(description = "Rental ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User full name snapshot", example = "Anna Ivanova")
    private String userFullName;

    @Schema(description = "Car ID", example = "2")
    private Long carId;

    @Schema(description = "Car info snapshot", example = "Toyota Camry (1234AB-7)")
    private String carInfo;

    @Schema(description = "Rental start date-time")
    private LocalDateTime startTime;

    @Schema(description = "Rental end date-time")
    private LocalDateTime endTime;

    @Schema(description = "Rental status", example = "ACTIVE")
    private String status;

    @Schema(description = "Selected extra service names")
    private List<String> selectedServices;
}
