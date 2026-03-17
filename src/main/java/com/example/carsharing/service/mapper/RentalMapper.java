package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        response.setUserId(rental.getUser().getId());
        response.setUserFullName(rental.getUser().getFirstName() + " " + rental.getUser().getLastName());
        response.setCarId(rental.getCar().getId());
        response.setCarInfo(rental.getCar().getBrand() + " " +
                rental.getCar().getModel() + " (" +
                rental.getCar().getLicensePlate() + ")");
        response.setStartTime(rental.getStartTime());
        response.setEndTime(rental.getEndTime());
        response.setStatus(rental.getStatus().name());
        response.setCreatedAt(rental.getCreatedAt());

        if (rental.getSelectedServices() != null) {
            response.setSelectedServices(
                    rental.getSelectedServices().stream()
                            .map(this::mapService)
                            .collect(Collectors.toList())
            );
        }

        if (rental.getEndTime() != null) {
            Rental.PriceDetails priceDetails = rental.getPriceDetails();
            if (priceDetails != null) {
                RentalResponse.PriceDetails details = new RentalResponse.PriceDetails();
                details.setCarAmount(priceDetails.getCarAmount());
                details.setServicesAmount(priceDetails.getServicesAmount());
                details.setTotalAmount(priceDetails.getTotalAmount());
                details.setRentalHours(priceDetails.getRentalHours());
                details.setRentalDays(priceDetails.getRentalDays());
                response.setPriceDetails(details);
            }
        }

        return response;
    }

    private RentalResponse.ServiceInfo mapService(ExtraService service) {
        RentalResponse.ServiceInfo info = new RentalResponse.ServiceInfo();
        info.setId(service.getId());
        info.setName(service.getName());
        info.setPricePerDay(service.getPricePerDay());
        info.setCategory(service.getCategory().name());
        return info;
    }
}