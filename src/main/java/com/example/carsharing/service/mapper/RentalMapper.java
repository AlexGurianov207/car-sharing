package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import org.springframework.stereotype.Component;

@Component
public class RentalMapper {

    public Rental toEntity(RentalCreateRequest request, User user, Car car) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        if (request.getStartTime() != null) {
            rental.setStartTime(request.getStartTime());
        }
        return rental;
    }

    public RentalResponse toResponse(Rental rental) {
        RentalResponse response = new RentalResponse();
        response.setId(rental.getId());
        response.setUserId(rental.getUser().getId());
        response.setUserFullName(rental.getUser().getFirstName() + " " + rental.getUser().getLastName());
        response.setCarId(rental.getCar().getId());
        response.setCarInfo(rental.getCar().getBrand() + " " +
                rental.getCar().getModel() + " (" +
                rental.getCar().getLicensePlate() + ")");
        response.setStartTime(rental.getStartTime());
        response.setEndTime(rental.getEndTime());
        response.setTotalPrice(rental.getTotalPrice());
        response.setStatus(rental.getStatus());
        response.setCreatedAt(rental.getCreatedAt());
        return response;
    }
}