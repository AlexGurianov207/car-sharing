package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.Car;
import org.springframework.stereotype.Component;

@Component
public class CarMapper {

    public Car toEntity(CarCreateRequest request) {
        Car car = new Car();
        car.setBrand(request.getBrand());
        car.setModel(request.getModel());
        car.setLicensePlate(request.getLicensePlate());
        car.setYear(request.getYear());
        car.setPricePerHour(request.getPricePerHour());
        return car;
    }

    public CarResponse toResponse(Car car) {
        CarResponse response = new CarResponse();
        response.setId(car.getId());
        response.setBrand(car.getBrand());
        response.setModel(car.getModel());
        response.setLicensePlate(car.getLicensePlate());
        response.setStatus(car.getStatus());
        response.setYear(car.getYear());
        response.setPricePerHour(car.getPricePerHour());
        return response;
    }
}