package com.example.carsharing.controller;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cars", description = "Operations for car management")
public class CarController {

    private final CarService carService;

    @GetMapping
    @Operation(summary = "Get all cars with optional filters")
    public List<CarResponse> getAllCars(
            @RequestParam(value = "status", required = false) CarStatus status,
            @RequestParam(value = "maxPrice", required = false)
            @Positive(message = "Max price must be positive") Double maxPrice,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "model", required = false) String model) {

        if (maxPrice != null) {
            return carService.findByMaxPrice(maxPrice);
        } else if (brand != null && model != null) {
            return carService.findByBrandAndModel(brand, model);
        } else {
            return carService.findAll(status);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car by ID")
    public CarResponse getCarById(@PathVariable @Positive(message = "Car ID must be positive") Long id) {
        return carService.findById(id);
    }

    @GetMapping("/by-license/{licensePlate}")
    @Operation(summary = "Get car by license plate")
    public CarResponse getCarByLicensePlate(@PathVariable String licensePlate) {
        return carService.findByLicensePlate(licensePlate);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create car")
    public CarResponse createCar(@Valid @RequestBody CarCreateRequest request) {
        return carService.createCar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update car")
    public CarResponse updateCar(
            @PathVariable @Positive(message = "Car ID must be positive") Long id,
            @Valid @RequestBody CarCreateRequest request) {
        return carService.updateCar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete car")
    public void deleteCar(@PathVariable @Positive(message = "Car ID must be positive") Long id) {
        carService.deleteCar(id);
    }

    @PutMapping("/{id}/available-services")
    @Operation(summary = "Update car available extra services")
    public CarResponse updateAvailableServices(
            @PathVariable @Positive(message = "Car ID must be positive") Long id,
            @NotNull(message = "Service IDs list is required")
            @Valid @RequestBody List<@NotNull(message = "Service ID cannot be null")
            @Positive(message = "Service ID must be positive") Long> serviceIds) {
        return carService.updateAvailableServices(id, serviceIds);
    }
}
