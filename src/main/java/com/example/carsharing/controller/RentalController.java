package com.example.carsharing.controller;

import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.service.RentalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Validated
public class RentalController {

    private final RentalService rentalService;

    @GetMapping("/{id}")
    public RentalResponse getRentalById(@PathVariable @Positive(message = "Rental ID must be positive") Long id) {
        return rentalService.getRentalById(id);
    }

    @GetMapping("/user/{userId}")
    public List<RentalResponse> getUserRentals(
            @PathVariable @Positive(message = "User ID must be positive") Long userId) {
        return rentalService.getUserRentals(userId);
    }

    @GetMapping("/car/{carId}")
    public List<RentalResponse> getCarRentals(
            @PathVariable @Positive(message = "Car ID must be positive") Long carId) {
        return rentalService.getCarRentals(carId);
    }

    @GetMapping("/active")
    public List<RentalResponse> getActiveRentals() {
        return rentalService.getActiveRentals();
    }

    @GetMapping("/search/jpql")
    public List<RentalResponse> searchRentalsJpql(
            @RequestParam(value = "carBrand", required = false) String carBrand,
            @RequestParam(value = "userId", required = false)
            @Positive(message = "User ID must be positive") Long userId,
            @RequestParam(value = "status", required = false) RentalStatus status) {
        return rentalService.searchRentalsJpql(carBrand, userId, status);
    }

    @GetMapping("/search/native")
    public List<RentalResponse> searchRentalsNative(
            @RequestParam(value = "carBrand", required = false) String carBrand,
            @RequestParam(value = "userId", required = false)
            @Positive(message = "User ID must be positive") Long userId,
            @RequestParam(value = "status", required = false) RentalStatus status) {
        return rentalService.searchRentalsNative(carBrand, userId, status);
    }

    @GetMapping("/search/paged")
    public Page<RentalResponse> getRentalsPage(Pageable pageable) {
        return rentalService.getRentalsPage(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse createRental(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRental(request);
    }

    @PatchMapping("/{id}/complete")
    public RentalResponse completeRental(@PathVariable @Positive(message = "Rental ID must be positive") Long id) {
        return rentalService.completeRental(id);
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
    public RentalResponse demoWithoutTransaction(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRentalWithoutTransaction(request);
    }

    @PostMapping("/demo/with-tx")
    public RentalResponse demoWithTransaction(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRentalWithTransaction(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRental(@PathVariable @Positive(message = "Rental ID must be positive") Long id) {
        rentalService.deleteRental(id);
    }
}
