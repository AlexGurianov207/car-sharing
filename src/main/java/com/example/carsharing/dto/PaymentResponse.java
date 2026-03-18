package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private Long rentalId;
    private Double amount;
    private Double carAmount;
    private Double servicesAmount;
    private LocalDateTime paymentDate;
    private String status;
    private String transactionId;
}