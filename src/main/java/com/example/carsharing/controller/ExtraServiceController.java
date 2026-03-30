package com.example.carsharing.controller;

import com.example.carsharing.dto.ExtraServiceCreateRequest;
import com.example.carsharing.dto.ExtraServiceResponse;
import com.example.carsharing.model.ServiceCategory;
import com.example.carsharing.service.ExtraServiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Validated
public class ExtraServiceController {

    private final ExtraServiceService extraServiceService;

    @GetMapping
    public List<ExtraServiceResponse> getAllServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) Boolean onlyActive) {
        return extraServiceService.getAllServices(category, onlyActive);
    }

    @GetMapping("/{id}")
    public ExtraServiceResponse getServiceById(
            @PathVariable @Positive(message = "Service ID must be positive") Long id) {
        return extraServiceService.getServiceById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExtraServiceResponse createService(@Valid @RequestBody ExtraServiceCreateRequest request) {
        return extraServiceService.createService(request);
    }

    @PutMapping("/{id}")
    public ExtraServiceResponse updateService(
            @PathVariable @Positive(message = "Service ID must be positive") Long id,
            @Valid @RequestBody ExtraServiceCreateRequest request) {
        return extraServiceService.updateService(id, request);
    }

    @PatchMapping("/{id}/status")
    public void updateServiceStatus(
            @PathVariable @Positive(message = "Service ID must be positive") Long id,
            @RequestParam @NotNull(message = "isActive is required") Boolean isActive) {
        extraServiceService.updateServiceStatus(id, isActive);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable @Positive(message = "Service ID must be positive") Long id) {
        extraServiceService.deleteService(id);
    }
}
