package com.example.carsharing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class RentalCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Car ID is required")
    private Long carId;

    private List<Long> serviceIds;
}