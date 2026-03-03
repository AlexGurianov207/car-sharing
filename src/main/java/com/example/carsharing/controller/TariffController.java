package com.example.carsharing.controller;

import com.example.carsharing.dto.TariffCreateRequest;
import com.example.carsharing.dto.TariffResponse;
import com.example.carsharing.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @GetMapping
    public List<TariffResponse> getAllTariffs(
            @RequestParam(value = "active", defaultValue = "false") boolean onlyActive) {
        return tariffService.getAllTariffs(onlyActive);
    }

    @GetMapping("/{id}")
    public TariffResponse getTariffById(@PathVariable Long id) {
        return tariffService.getTariffById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse createTariff(@Valid @RequestBody TariffCreateRequest request) {
        return tariffService.createTariff(request);
    }

    @PutMapping("/{id}")
    public TariffResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffCreateRequest request) {
        return tariffService.updateTariff(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivateTariff(@PathVariable Long id) {
        tariffService.deactivateTariff(id);
    }
}