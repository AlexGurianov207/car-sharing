package com.example.carsharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExtraServiceResponse {
    private Long id;
    private String name;
    private String description;
    private Double pricePerDay;
    private String category;
    private Boolean isActive;
    private LocalDateTime createdAt;
}