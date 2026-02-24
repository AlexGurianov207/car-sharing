package com.example.carsharing.service;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.service.mapper.CarMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    public CarResponse createCar(CarCreateRequest request) {
        carRepository.findByLicensePlate(request.getLicensePlate())
                .ifPresent(car -> {
                    throw new IllegalArgumentException("Car with license plate " +
                            request.getLicensePlate() + " already exists");
                });

        Car car = carMapper.toEntity(request);

        Car savedCar = carRepository.save(car);

        return carMapper.toResponse(savedCar);
    }

    public CarResponse findById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        return carMapper.toResponse(car);
    }

    public List<CarResponse> findAll(String status) {
        List<Car> cars = carRepository.findAll();

        if (status != null && !status.isEmpty()) {
            cars = cars.stream()
                    .filter(car -> status.equals(car.getStatus()))
                    .toList();
        }

        return cars.stream()
                .map(carMapper::toResponse)
                .toList();
    }

    public CarResponse updateStatus(Long id, String status) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));

        car.setStatus(status);
        Car updatedCar = carRepository.save(car);

        return carMapper.toResponse(updatedCar);
    }

    public void deleteCar(Long id) throws NoSuchFieldException {
        if (!carRepository.findById(id).isPresent()) {
            throw new NoSuchFieldException("Car not found with id: " + id);
        }
        carRepository.deleteById(id);
    }
}