package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.TariffCreateRequest;
import com.example.carsharing.dto.TariffResponse;
import com.example.carsharing.model.Tariff;
import org.springframework.stereotype.Component;

@Component
public class TariffMapper {

    public Tariff toEntity(TariffCreateRequest request) {
        Tariff tariff = new Tariff();
        tariff.setName(request.getName());
        tariff.setDescription(request.getDescription());
        tariff.setPricePerHour(request.getPricePerHour());
        tariff.setPricePerDay(request.getPricePerDay());
        tariff.setMinRentalHours(request.getMinRentalHours());
        tariff.setMaxRentalDays(request.getMaxRentalDays());
        tariff.setIsActive(request.getIsActive());
        return tariff;
    }

    public TariffResponse toResponse(Tariff tariff) {
        TariffResponse response = new TariffResponse();
        response.setId(tariff.getId());
        response.setName(tariff.getName());
        response.setDescription(tariff.getDescription());
        response.setPricePerHour(tariff.getPricePerHour());
        response.setPricePerDay(tariff.getPricePerDay());
        response.setMinRentalHours(tariff.getMinRentalHours());
        response.setMaxRentalDays(tariff.getMaxRentalDays());
        response.setIsActive(tariff.getIsActive());
        response.setCreatedAt(tariff.getCreatedAt());
        return response;
    }
}