package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RentalMapper {

    public Rental toEntity(User user, Car car) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());
        return rental;
    }

    public RentalResponse toResponse(Rental rental) {
        RentalResponse response = new RentalResponse();
        response.setId(rental.getId());

        if (rental.getUser() != null) {
            response.setUserId(rental.getUser().getId());
            response.setUserFullName(rental.getUser().getFirstName() + " " +
                    rental.getUser().getLastName());
        } else {
            response.setUserId(null);
            response.setUserFullName(rental.getUserFullName());
        }

        if (rental.getCar() != null) {
            response.setCarId(rental.getCar().getId());
            response.setCarInfo(rental.getCar().getBrand() + " " +
                    rental.getCar().getModel() + " (" +
                    rental.getCar().getLicensePlate() + ")");
        } else {
            response.setCarId(null);
            response.setCarInfo(rental.getCarInfo());
        }

        response.setStartTime(rental.getStartTime());
        response.setEndTime(rental.getEndTime());
        response.setStatus(rental.getStatus().name());

        List<String> serviceNameList = new ArrayList<>();

        if (rental.getSelectedServices() != null && !rental.getSelectedServices().isEmpty()) {
            serviceNameList = rental.getSelectedServices().stream()
                    .map(ExtraService::getName)
                    .collect(Collectors.toList());
        } else if (rental.getServiceNames() != null && !rental.getServiceNames().isEmpty()) {
            String[] names = rental.getServiceNames().split(",");
            serviceNameList = Arrays.stream(names)
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        response.setSelectedServices(serviceNameList);

        return response;
    }
}