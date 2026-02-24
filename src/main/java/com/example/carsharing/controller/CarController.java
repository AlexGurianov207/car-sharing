package com.example.carsharing.controller;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.service.CarService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping
    public List<CarResponse> getAllCars(
            @RequestParam(value = "status", required = false) String status) {
        return carService.findAll(status);
    }

    @GetMapping("/{id}")
    public CarResponse getCarById(@PathVariable Long id) {
        return carService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponse createCar(@RequestBody CarCreateRequest request) {
        return carService.createCar(request);
    }

    @PatchMapping("/{id}/status")
    public CarResponse updateCarStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return carService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCar(@PathVariable Long id) throws NoSuchFieldException {
        carService.deleteCar(id);
    }
}