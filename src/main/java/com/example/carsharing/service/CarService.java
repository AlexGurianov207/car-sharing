package com.example.carsharing.service;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.service.mapper.CarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final ExtraServiceRepository extraServiceRepository;
    private final RentalRepository rentalRepository;
    private final RentalService rentalService;

    private static final String CAR_NOT_FOUND_MESSAGE = "Car not found with";
    private static final String ID_MESSAGE = " id: ";

    private void checkNotRented(Long carId, Car car) {
        if (car.getStatus() == CarStatus.RENTED) {
            boolean hasActiveRental = rentalRepository.existsByCarIdAndEndTimeIsNull(carId);
            if (hasActiveRental) {
                throw new InvalidDataAccessApiUsageException(
                        "Cannot modify car with active rental");
            }
        }
    }

    public CarResponse createCar(CarCreateRequest request) {
        if (carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DataIntegrityViolationException("Car with license plate "
                    + request.getLicensePlate() + " already exists");
        }

        Car car = carMapper.toEntity(request);
        Car savedCar = carRepository.save(car);
        rentalService.invalidateSearchIndex();
        return carMapper.toResponse(savedCar);
    }

    public CarResponse findById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CAR_NOT_FOUND_MESSAGE + ID_MESSAGE + id));
        return carMapper.toResponse(car);
    }

    public CarResponse findByLicensePlate(String licensePlate) {
        Car car = carRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new RuntimeException(CAR_NOT_FOUND_MESSAGE + " license plate: " + licensePlate));
        return carMapper.toResponse(car);
    }

    public List<CarResponse> findAll(CarStatus status) {
        List<Car> cars;
        if (status != null) {
            cars = carRepository.findByStatus(status);
        } else {
            cars = carRepository.findAll();
        }

        return cars.stream()
                .map(carMapper::toResponse)
                .toList();
    }

    public List<CarResponse> findByMaxPrice(Double maxPrice) {
        return carRepository.findByPricePerHourLessThanEqual(maxPrice).stream()
                .map(carMapper::toResponse)
                .toList();
    }

    public List<CarResponse> findByBrandAndModel(String brand, String model) {
        return carRepository.findByBrandAndModel(brand, model).stream()
                .map(carMapper::toResponse)
                .toList();
    }

    public CarResponse updateCar(Long id, CarCreateRequest request) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CAR_NOT_FOUND_MESSAGE + ID_MESSAGE + id));

        if (car.getStatus() == CarStatus.DELETED) {
            throw new InvalidDataAccessApiUsageException("Cannot update deleted car");
        }

        checkNotRented(id, car);

        if (!car.getLicensePlate().equals(request.getLicensePlate()) &&
                carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DataIntegrityViolationException("License plate "
                    + request.getLicensePlate() + " is already taken");
        }

        car.setBrand(request.getBrand());
        car.setModel(request.getModel());
        car.setLicensePlate(request.getLicensePlate());
        car.setYear(request.getYear());
        car.setPricePerHour(request.getPricePerHour());

        Car updatedCar = carRepository.save(car);
        rentalService.invalidateSearchIndex();
        return carMapper.toResponse(updatedCar);
    }

    public void deleteCar(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Car not found"));

        if (car.getStatus() == CarStatus.DELETED) {
            return;
        }

        if (car.getStatus() == CarStatus.RENTED) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot delete rented car");
        }

        if (rentalRepository.existsByCarId(id)) {
            car.setStatus(CarStatus.DELETED);
            carRepository.save(car);
            rentalService.invalidateSearchIndex();
            return;
        }

        carRepository.deleteById(id);
        rentalService.invalidateSearchIndex();
    }

    public CarResponse updateAvailableServices(Long carId, List<Long> serviceIds) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException(CAR_NOT_FOUND_MESSAGE + ID_MESSAGE + carId));

        if (car.getStatus() == CarStatus.DELETED) {
            throw new InvalidDataAccessApiUsageException("Cannot update deleted car");
        }

        checkNotRented(carId, car);

        List<ExtraService> services = extraServiceRepository.findAllById(serviceIds);

        if (services.size() != serviceIds.size()) {
            throw new DataIntegrityViolationException("Some services not found");
        }

        List<ExtraService> inactiveServices = services.stream()
                .filter(service -> !service.getIsActive())
                .toList();

        if (!inactiveServices.isEmpty()) {
            String inactiveNames = inactiveServices.stream()
                    .map(ExtraService::getName)
                    .collect(Collectors.joining(", "));
            throw new InvalidDataAccessApiUsageException(
                    "Cannot add inactive services: " + inactiveNames);
        }

        car.setAvailableServices(services);
        Car updatedCar = carRepository.save(car);
        rentalService.invalidateSearchIndex();

        return carMapper.toResponse(updatedCar);
    }
}
