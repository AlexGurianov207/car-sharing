package com.example.carsharing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class RentalCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Car ID is required")
    private Long carId;

    // Можно указать конкретное время начала, иначе будет сейчас
    private java.time.LocalDateTime startTime;
}