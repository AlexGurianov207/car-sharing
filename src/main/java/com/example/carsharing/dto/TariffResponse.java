package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TariffResponse {
    private Long id;
    private String name;
    private String description;
    private Double pricePerHour;
    private Double pricePerDay;
    private Integer minRentalHours;
    private Integer maxRentalDays;
    private Boolean isActive;
    private LocalDateTime createdAt;
}