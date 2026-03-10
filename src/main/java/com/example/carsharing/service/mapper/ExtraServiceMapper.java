package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.ExtraServiceCreateRequest;
import com.example.carsharing.dto.ExtraServiceResponse;
import com.example.carsharing.model.ExtraService;
import org.springframework.stereotype.Component;

@Component
public class ExtraServiceMapper {

    public ExtraService toEntity(ExtraServiceCreateRequest request) {
        ExtraService service = new ExtraService();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPricePerDay(request.getPricePerDay());
        service.setCategory(request.getCategory());
        service.setIsActive(request.getIsActive());
        return service;
    }

    public ExtraServiceResponse toResponse(ExtraService service) {
        ExtraServiceResponse response = new ExtraServiceResponse();
        response.setId(service.getId());
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setPricePerDay(service.getPricePerDay());
        response.setCategory(service.getCategory());
        response.setIsActive(service.getIsActive());
        response.setCreatedAt(service.getCreatedAt());
        return response;
    }
}