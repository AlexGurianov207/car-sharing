package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private Long rentalId;
    private Long userId;
    private String userFullName;
    private Double amount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String status;
    private String transactionId;
}