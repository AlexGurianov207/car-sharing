package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request for creating a rental")
public class RentalCreateRequest {

    @Schema(description = "User ID", example = "1")
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @Schema(description = "Car ID", example = "2")
    @NotNull(message = "Car ID is required")
    @Positive(message = "Car ID must be positive")
    private Long carId;

    @ArraySchema(schema = @Schema(description = "Extra service ID", example = "10"))
    private List<@NotNull(message = "Service ID cannot be null")
            @Positive(message = "Service ID must be positive") Long> serviceIds;
}
