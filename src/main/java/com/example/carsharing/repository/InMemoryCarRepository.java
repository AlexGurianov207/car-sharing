package com.example.carsharing.repository;

import com.example.carsharing.model.Car;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCarRepository implements CarRepository {

    private final Map<Long, Car> storage = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Car save(Car car) {
        if (car.getId() == null) {
            car.setId(idGenerator.getAndIncrement());
        }

        if (car.getStatus() == null) {
            car.setStatus("AVAILABLE");
        }

        storage.put(car.getId(), car);
        return car;
    }

    @Override
    public Optional<Car> findById(Long id) {
        Car car = storage.get(id);
        if (car == null) {
            return Optional.empty();
        }
        return Optional.of(car);
    }

    @Override
    public List<Car> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<Car> findByLicensePlate(String licensePlate) {
        for (Car car : storage.values()) {
            if (car.getLicensePlate().equals(licensePlate)) {
                return Optional.of(car);
            }
        }
        return Optional.empty();
    }

}
