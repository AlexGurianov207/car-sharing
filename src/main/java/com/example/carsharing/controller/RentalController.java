package com.example.carsharing.controller;

import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @GetMapping("/{id}")
    public RentalResponse getRentalById(@PathVariable Long id) {
        return rentalService.getRentalById(id);
    }

    @GetMapping("/user/{userId}")
    public List<RentalResponse> getUserRentals(@PathVariable Long userId) {
        return rentalService.getUserRentals(userId);
    }

    @GetMapping("/car/{carId}")
    public List<RentalResponse> getCarRentals(@PathVariable Long carId) {
        return rentalService.getCarRentals(carId);
    }

    @GetMapping("/active")
    public List<RentalResponse> getActiveRentals() {
        return rentalService.getActiveRentals();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse createRental(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRental(request);
    }

    @PatchMapping("/{id}/complete")
    public RentalResponse completeRental(@PathVariable Long id) {
        return rentalService.completeRental(id);
    }

    @PatchMapping("/{id}/cancel")
    public RentalResponse cancelRental(@PathVariable Long id) {
        return rentalService.cancelRental(id);
    }

    @GetMapping("/demo/n-plus-one")
    public List<RentalResponse> demonstrateNPlus1() {
        return rentalService.demonstrateNPlus1Problem();
    }

    @GetMapping("/demo/solution")
    public List<RentalResponse> demonstrateSolution() {
        return rentalService.demonstrateSolutionWithEntityGraph();
    }

    @PostMapping("/demo/without-tx")
    public RentalResponse demoWithoutTransaction(@RequestBody RentalCreateRequest request) {
        return rentalService.createRentalWithoutTransaction(request);
    }

    @PostMapping("/demo/with-tx")
    public RentalResponse demoWithTransaction(@RequestBody RentalCreateRequest request) {
        return rentalService.createRentalWithTransaction(request);
    }
}