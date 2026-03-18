package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RentalResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private Long carId;
    private String carInfo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private List<String> selectedServices;
}