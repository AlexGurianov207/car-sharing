package com.example.carsharing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

@Data
public class PaymentCreateRequest {

    @NotNull(message = "Rental ID is required")
    private Long rentalId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String transactionId;
}