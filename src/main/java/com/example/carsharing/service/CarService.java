package com.example.carsharing.service;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.service.mapper.CarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    public CarResponse createCar(CarCreateRequest request) {
        if (carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new RuntimeException("Car with license plate " + request.getLicensePlate() + " already exists");
        }

        Car car = carMapper.toEntity(request);
        Car savedCar = carRepository.save(car);
        return carMapper.toResponse(savedCar);
    }

    public CarResponse findById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        return carMapper.toResponse(car);
    }

    public CarResponse findByLicensePlate(String licensePlate) {
        Car car = carRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new RuntimeException("Car not found with license plate: " + licensePlate));
        return carMapper.toResponse(car);
    }

    public List<CarResponse> findAll(String status) {
        List<Car> cars;
        if (status != null && !status.isEmpty()) {
            cars = carRepository.findByStatus(status);
        } else {
            cars = carRepository.findAll();
        }

        return cars.stream()
                .map(carMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CarResponse> findByMaxPrice(Double maxPrice) {
        return carRepository.findByPricePerHourLessThanEqual(maxPrice).stream()
                .map(carMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CarResponse> findByBrandAndModel(String brand, String model) {
        return carRepository.findByBrandAndModel(brand, model).stream()
                .map(carMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CarResponse updateStatus(Long id, String status) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));

        car.setStatus(status);
        Car updatedCar = carRepository.save(car);

        return carMapper.toResponse(updatedCar);
    }

    public CarResponse updateCar(Long id, CarCreateRequest request) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));

        // Проверяем, не занят ли новый госномер другой машиной
        if (!car.getLicensePlate().equals(request.getLicensePlate()) &&
                carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new RuntimeException("License plate " + request.getLicensePlate() + " is already taken");
        }

        car.setBrand(request.getBrand());
        car.setModel(request.getModel());
        car.setLicensePlate(request.getLicensePlate());
        car.setYear(request.getYear());
        car.setPricePerHour(request.getPricePerHour());
        // Статус не меняем через этот метод

        Car updatedCar = carRepository.save(car);
        return carMapper.toResponse(updatedCar);
    }

    public void deleteCar(Long id) {
        if (!carRepository.existsById(id)) {
            throw new RuntimeException("Car not found with id: " + id);
        }
        carRepository.deleteById(id);
    }
}