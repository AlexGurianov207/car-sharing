package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RentalResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private Long carId;
    private String carInfo;  // Например: "BMW X5 (A123BC)"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalPrice;
    private String status;
    private LocalDateTime createdAt;
}