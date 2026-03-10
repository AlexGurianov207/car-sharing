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
    private String carInfo;  // Например: "BMW X5 (A123BC)"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createdAt;

    // НОВОЕ: Информация о выбранных услугах
    private List<ServiceInfo> selectedServices;

    // НОВОЕ: Детализация цен (будет null, если аренда не завершена)
    private PriceDetails priceDetails;

    @Data
    public static class ServiceInfo {
        private Long id;
        private String name;
        private Double pricePerDay;
        private String category;
    }

    @Data
    public static class PriceDetails {
        private Double carAmount;
        private Double servicesAmount;
        private Double totalAmount;
        private Long rentalHours;
        private Long rentalDays;
    }
}