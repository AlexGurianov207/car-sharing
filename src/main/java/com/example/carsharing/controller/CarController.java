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

@RestController  // Объединяет @Controller и @ResponseBody
@RequestMapping("/api/cars")  // Базовый URL для всех методов в этом контроллере
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    // GET /api/cars - получить все машины (с возможностью фильтрации по статусу)
    // Пример: GET /api/cars?status=AVAILABLE
    @GetMapping
    public List<CarResponse> getAllCars(
            @RequestParam(value = "status", required = false) String status) {
        return carService.findAll(status);
    }

    // GET /api/cars/{id} - получить машину по ID
    // Пример: GET /api/cars/1
    @GetMapping("/{id}")
    public CarResponse getCarById(@PathVariable Long id) {
        return carService.findById(id);
    }

    // POST /api/cars - создать новую машину
    // Пример: POST /api/cars с JSON телом запроса
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)  // Возвращаем статус 201 Created
    public CarResponse createCar(@RequestBody CarCreateRequest request) {
        return carService.createCar(request);
    }

    // PATCH /api/cars/{id}/status - обновить статус машины
    // Пример: PATCH /api/cars/1/status?status=RENTED
    @PatchMapping("/{id}/status")
    public CarResponse updateCarStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return carService.updateStatus(id, status);
    }

    // DELETE /api/cars/{id} - удалить машину
    // Пример: DELETE /api/cars/1
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // Возвращаем статус 204 No Content
    public void deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
    }
}