package com.example.carsharing.service;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.service.mapper.CarMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    public CarResponse createCar(CarCreateRequest request) {
        Car car = carMapper.toEntity(request);

        Car savedCar = carRepository.save(car);

        return carMapper.toResponse(savedCar);
    }

    public CarResponse findById(Long id) {
        Optional<Car> optionalCar = carRepository.findById(id);

        if (!optionalCar.isPresent()) {
            return  null;
        }

        Car car = optionalCar.get();
        return carMapper.toResponse(car);
    }

    public List<CarResponse> findAll() {
        List<Car> cars = carRepository.findAll();
        List<CarResponse> responses = new ArrayList<>();

        for (Car car : cars) {
            responses.add(carMapper.toResponse(car));
        }

        return responses;
    }

    public List<CarResponse> findByBrand(String brand) {
        List<Car> cars = carRepository.findAll();
        List<CarResponse> responses = new ArrayList<>();

        for (Car car : cars) {
            if (brand.equalsIgnoreCase(car.getBrand())) {
                responses.add(carMapper.toResponse(car));
            }
        }

        return responses;
    }

}