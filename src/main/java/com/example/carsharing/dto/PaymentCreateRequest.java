package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Request for creating payment")
public class PaymentCreateRequest {

    @Schema(description = "Rental ID", example = "1")
    @NotNull(message = "Rental ID is required")
    private Long rentalId;

    @Schema(description = "User ID", example = "1")
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Payment amount", example = "75.5")
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @Schema(description = "Payment transaction ID", example = "TXN-1A2B3C4D")
    private String transactionId;
}
