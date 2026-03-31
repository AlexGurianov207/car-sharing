package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Payment response")
public class PaymentResponse {
    @Schema(description = "Payment ID", example = "1")
    private Long id;

    @Schema(description = "Rental ID", example = "1")
    private Long rentalId;

    @Schema(description = "Total amount", example = "75.5")
    private Double amount;

    @Schema(description = "Car amount", example = "60.0")
    private Double carAmount;

    @Schema(description = "Services amount", example = "15.5")
    private Double servicesAmount;

    @Schema(description = "Payment date-time")
    private LocalDateTime paymentDate;

    @Schema(description = "Payment status", example = "COMPLETED")
    private String status;

    @Schema(description = "Transaction ID", example = "TXN-1A2B3C4D")
    private String transactionId;
}
