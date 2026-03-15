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
    private Double carAmount;
    private Double servicesAmount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String status;
    private String transactionId;

    private String carInfo;
    private LocalDateTime rentalStartTime;
    private LocalDateTime rentalEndTime;
    private Long rentalHours;
}