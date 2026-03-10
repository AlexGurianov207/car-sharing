package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private Long rentalId;
    private Long userId;
    private String userFullName;
    private Double amount;  // Общая сумма
    private Double carAmount;  // Сумма за машину
    private Double servicesAmount;  // Сумма за услуги
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String status;
    private String transactionId;

    // НОВОЕ: Детали аренды для удобства
    private String carInfo;
    private LocalDateTime rentalStartTime;
    private LocalDateTime rentalEndTime;
    private Long rentalHours;
}