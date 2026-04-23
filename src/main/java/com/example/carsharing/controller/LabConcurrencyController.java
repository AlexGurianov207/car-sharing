package com.example.carsharing.controller;

import com.example.carsharing.dto.CounterValueResponse;
import com.example.carsharing.service.ThreadSafeCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab/counter")
@RequiredArgsConstructor
@Validated
@Tag(name = "Lab Concurrency", description = "Endpoints for concurrency lab demonstrations")
public class LabConcurrencyController {

    private final ThreadSafeCounterService threadSafeCounterService;

    @PostMapping("/increment")
    @Operation(summary = "Increment counter by 1")
    public CounterValueResponse increment() {
        return new CounterValueResponse(threadSafeCounterService.incrementAndGet());
    }

    @PostMapping("/increment/{times}")
    @Operation(summary = "Increment counter by N")
    public CounterValueResponse incrementMany(
            @PathVariable @Positive(message = "times must be positive") int times) {
        return new CounterValueResponse(threadSafeCounterService.incrementManyAndGet(times));
    }

    @GetMapping
    @Operation(summary = "Get current counter value")
    public CounterValueResponse getValue() {
        return new CounterValueResponse(threadSafeCounterService.getValue());
    }

    @PatchMapping("/reset")
    @Operation(summary = "Reset counter to 0")
    public CounterValueResponse reset() {
        return new CounterValueResponse(threadSafeCounterService.resetAndGet());
    }
}
