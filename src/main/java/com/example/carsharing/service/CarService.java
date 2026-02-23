package com.example.carsharing.service;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.service.mapper.CarMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service  // Говорит Spring, что это сервис (бизнес-логика)
@RequiredArgsConstructor  // Lombok создаст конструктор для всех final полей
public class CarService {

    private final CarRepository carRepository;  // Внедряем зависимость
    private final CarMapper carMapper;          // Внедряем зависимость

    // Создание новой машины
    public CarResponse createCar(CarCreateRequest request) {
        // 1. Проверяем, нет ли уже машины с таким госномером
        carRepository.findByLicensePlate(request.getLicensePlate())
                .ifPresent(car -> {
                    throw new IllegalArgumentException("Car with license plate " +
                            request.getLicensePlate() + " already exists");
                });

        // 2. Преобразуем DTO в сущность
        Car car = carMapper.toEntity(request);

        // 3. Сохраняем в репозитории
        Car savedCar = carRepository.save(car);

        // 4. Преобразуем обратно в DTO и возвращаем
        return carMapper.toResponse(savedCar);
    }

    // Получение машины по ID
    public CarResponse findById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        return carMapper.toResponse(car);
    }

    // Получение всех машин (с возможностью фильтрации по статусу)
    public List<CarResponse> findAll(String status) {
        List<Car> cars = carRepository.findAll();

        // Если указан статус - фильтруем
        if (status != null && !status.isEmpty()) {
            cars = cars.stream()
                    .filter(car -> status.equals(car.getStatus()))
                    .toList();
        }

        // Преобразуем все машины в DTO
        return cars.stream()
                .map(carMapper::toResponse)
                .toList();
    }

    // Обновление статуса машины
    public CarResponse updateStatus(Long id, String status) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));

        car.setStatus(status);
        Car updatedCar = carRepository.save(car);

        return carMapper.toResponse(updatedCar);
    }

    // Удаление машины
    public void deleteCar(Long id) {
        if (!carRepository.findById(id).isPresent()) {
            throw new RuntimeException("Car not found with id: " + id);
        }
        carRepository.deleteById(id);
    }
}