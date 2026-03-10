package com.example.carsharing.service;

import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.RentalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final ExtraServiceRepository extraServiceRepository;  // НОВОЕ
    private final RentalMapper rentalMapper;

    public RentalResponse createRental(RentalCreateRequest request) {
        // Проверяем существование пользователя
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        // Проверяем, активен ли пользователь
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active. Status: " + user.getStatus());
        }

        // Проверяем существование машины
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + request.getCarId()));

        // Проверяем, доступна ли машина
        if (!"AVAILABLE".equals(car.getStatus())) {
            throw new RuntimeException("Car is not available. Status: " + car.getStatus());
        }

        // Проверяем, нет ли уже активной аренды на эту машину
        if (rentalRepository.existsByCarIdAndEndTimeIsNull(car.getId())) {
            throw new RuntimeException("Car is already rented");
        }

        // Создаем аренду
        Rental rental = rentalMapper.toEntity(request, user, car);

        // НОВОЕ: Добавляем выбранные услуги
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());
            // Проверяем, что все услуги доступны у этой машины
            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    throw new RuntimeException("Service " + service.getName() + " is not available for this car");
                }
            }
            rental.setSelectedServices(services);
        }

        Rental savedRental = rentalRepository.save(rental);

        // Меняем статус машины
        car.setStatus("RENTED");
        carRepository.save(car);

        return rentalMapper.toResponse(savedRental);
    }

    public RentalResponse completeRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + id));

        if (!"ACTIVE".equals(rental.getStatus())) {
            throw new RuntimeException("Rental is not active. Status: " + rental.getStatus());
        }

        // Устанавливаем время окончания
        rental.setEndTime(LocalDateTime.now());
        rental.setStatus("COMPLETED");

        // Освобождаем машину
        Car car = rental.getCar();
        car.setStatus("AVAILABLE");
        carRepository.save(car);

        Rental updatedRental = rentalRepository.save(rental);
        return rentalMapper.toResponse(updatedRental);
    }

    // НОВОЕ: Метод для демонстрации N+1 проблемы
    public List<RentalResponse> getAllRentalsWithNPlus1Problem() {
        // Этот метод будет демонстрировать проблему N+1
        // (детали реализации добавим позже)
        return rentalRepository.findAll().stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }

    // НОВОЕ: Метод с решением N+1 проблемы через @EntityGraph
    public List<RentalResponse> getAllRentalsWithDetails() {
        // Этот метод будет использовать @EntityGraph
        // (детали реализации добавим позже)
        return rentalRepository.findAll().stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RentalResponse cancelRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + id));

        if (!"ACTIVE".equals(rental.getStatus())) {
            throw new RuntimeException("Rental is not active. Status: " + rental.getStatus());
        }

        rental.setStatus("CANCELLED");
        rental.setEndTime(LocalDateTime.now());

        // Освобождаем машину
        Car car = rental.getCar();
        car.setStatus("AVAILABLE");
        carRepository.save(car);

        Rental updatedRental = rentalRepository.save(rental);
        return rentalMapper.toResponse(updatedRental);
    }

    public RentalResponse getRentalById(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + id));
        return rentalMapper.toResponse(rental);
    }

    public List<RentalResponse> getUserRentals(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        return rentalRepository.findByUserId(userId).stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RentalResponse> getCarRentals(Long carId) {
        if (!carRepository.existsById(carId)) {
            throw new RuntimeException("Car not found with id: " + carId);
        }

        return rentalRepository.findByCarId(carId).stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RentalResponse> getActiveRentals() {
        return rentalRepository.findByEndTimeIsNull().stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }
}