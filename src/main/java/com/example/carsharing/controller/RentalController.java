package com.example.carsharing.controller;

import com.example.carsharing.dto.BulkRentalResponse;
import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
@Tag(name = "Rentals", description = "Operations for rentals")
public class RentalController {

    private final RentalService rentalService;

    @GetMapping("/{id}")
    @Operation(summary = "Get rental by ID")
    public RentalResponse getRentalById(@PathVariable @Positive(message = "Rental ID must be positive") Long id) {
        return rentalService.getRentalById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get rentals by user ID")
    public List<RentalResponse> getUserRentals(
            @PathVariable @Positive(message = "User ID must be positive") Long userId) {
        return rentalService.getUserRentals(userId);
    }

    @GetMapping("/car/{carId}")
    @Operation(summary = "Get rentals by car ID")
    public List<RentalResponse> getCarRentals(
            @PathVariable @Positive(message = "Car ID must be positive") Long carId) {
        return rentalService.getCarRentals(carId);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active rentals")
    public List<RentalResponse> getActiveRentals() {
        return rentalService.getActiveRentals();
    }

    @GetMapping("/search/jpql")
    @Operation(summary = "Search rentals using JPQL")
    public List<RentalResponse> searchRentalsJpql(
            @RequestParam(value = "carBrand", required = false) String carBrand,
            @RequestParam(value = "userId", required = false)
            @Positive(message = "User ID must be positive") Long userId,
            @RequestParam(value = "status", required = false) RentalStatus status) {
        return rentalService.searchRentalsJpql(carBrand, userId, status);
    }

    @GetMapping("/search/native")
    @Operation(summary = "Search rentals using native SQL")
    public List<RentalResponse> searchRentalsNative(
            @RequestParam(value = "carBrand", required = false) String carBrand,
            @RequestParam(value = "userId", required = false)
            @Positive(message = "User ID must be positive") Long userId,
            @RequestParam(value = "status", required = false) RentalStatus status) {
        return rentalService.searchRentalsNative(carBrand, userId, status);
    }

    @GetMapping("/search/paged")
    @Operation(summary = "Get rentals page")
    public Page<RentalResponse> getRentalsPage(Pageable pageable) {
        return rentalService.getRentalsPage(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create rental")
    public RentalResponse createRental(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRental(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create rentals in bulk")
    public BulkRentalResponse createRentalsBulk(
            @RequestBody @NotEmpty(message = "Rentals list cannot be empty")
            List<@Valid RentalCreateRequest> requests) {
        return rentalService.createRentalsBulk(requests);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete rental")
    public RentalResponse completeRental(@PathVariable @Positive(message = "Rental ID must be positive") Long id) {
        return rentalService.completeRental(id);
    }

    @GetMapping("/demo/n-plus-one")
    @Operation(summary = "Demo endpoint for N+1 problem")
    public List<RentalResponse> demonstrateNPlus1() {
        return rentalService.demonstrateNPlus1Problem();
    }

    @GetMapping("/demo/solution")
    @Operation(summary = "Demo endpoint for N+1 solution")
    public List<RentalResponse> demonstrateSolution() {
        return rentalService.demonstrateSolutionWithEntityGraph();
    }

    @PostMapping("/demo/without-tx")
    @Operation(summary = "Demo rental creation without transaction")
    public RentalResponse demoWithoutTransaction(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRentalWithoutTransaction(request);
    }

    @PostMapping("/demo/with-tx")
    @Operation(summary = "Demo rental creation with transaction")
    public RentalResponse demoWithTransaction(@Valid @RequestBody RentalCreateRequest request) {
        return rentalService.createRentalWithTransaction(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete rental")
    public void deleteRental(@PathVariable @Positive(message = "Rental ID must be positive") Long id) {
        rentalService.deleteRental(id);
    }
}
