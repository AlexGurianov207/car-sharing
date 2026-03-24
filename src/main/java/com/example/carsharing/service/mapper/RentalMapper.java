package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
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
        boolean useSnapshot = rental.getStatus() == RentalStatus.COMPLETED;
        applyUserData(response, rental, useSnapshot);
        applyCarData(response, rental, useSnapshot);

        response.setStartTime(rental.getStartTime());
        response.setEndTime(rental.getEndTime());
        response.setStatus(rental.getStatus().name());
        response.setSelectedServices(resolveServiceNames(rental, useSnapshot));

        return response;
    }

    private void applyUserData(RentalResponse response, Rental rental, boolean useSnapshot) {
        if (useSnapshot && hasText(rental.getUserFullName())) {
            response.setUserId(rental.getUser() != null ? rental.getUser().getId() : null);
            response.setUserFullName(rental.getUserFullName());
            return;
        }

        if (rental.getUser() != null) {
            response.setUserId(rental.getUser().getId());
            response.setUserFullName(rental.getUser().getFirstName() + " "
                    + rental.getUser().getLastName());
            return;
        }

        response.setUserId(null);
        response.setUserFullName(rental.getUserFullName());
    }

    private void applyCarData(RentalResponse response, Rental rental, boolean useSnapshot) {
        if (useSnapshot && hasText(rental.getCarInfo())) {
            response.setCarId(rental.getCar() != null ? rental.getCar().getId() : null);
            response.setCarInfo(rental.getCarInfo());
            return;
        }

        if (rental.getCar() != null) {
            response.setCarId(rental.getCar().getId());
            response.setCarInfo(rental.getCar().getBrand() + " "
                    + rental.getCar().getModel() + " ("
                    + rental.getCar().getLicensePlate() + ")");
            return;
        }

        response.setCarId(null);
        response.setCarInfo(rental.getCarInfo());
    }

    private List<String> resolveServiceNames(Rental rental, boolean useSnapshot) {
        if (useSnapshot && hasText(rental.getServiceNames())) {
            return splitServiceNames(rental.getServiceNames(), "[;,]", true);
        }

        if (rental.getSelectedServices() != null && !rental.getSelectedServices().isEmpty()) {
            return rental.getSelectedServices().stream()
                    .map(ExtraService::getName)
                    .collect(Collectors.toList());
        }

        if (hasText(rental.getServiceNames())) {
            return splitServiceNames(rental.getServiceNames(), ",", false);
        }

        return new ArrayList<>();
    }

    private List<String> splitServiceNames(String serviceNames, String delimiter, boolean skipEmptyNames) {
        return Arrays.stream(serviceNames.split(delimiter))
                .map(String::trim)
                .filter(name -> !skipEmptyNames || !name.isEmpty())
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
