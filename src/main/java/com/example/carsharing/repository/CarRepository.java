package com.example.carsharing.repository;

import com.example.carsharing.model.Car;

import java.util.List;
import java.util.Optional;

public interface CarRepository {
    Car save(Car car);

    Optional<Car> findById(Long id);

    List<Car> findAll();

    Optional<Car> findByLicensePlate(String licensePlate);
}
