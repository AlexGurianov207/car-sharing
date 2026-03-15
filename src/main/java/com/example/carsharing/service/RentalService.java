package com.example.carsharing.service;

import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.RentalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final ExtraServiceRepository extraServiceRepository;
    private final RentalMapper rentalMapper;

    @Transactional
    public RentalResponse createRental(RentalCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("User is not active. Status: " + user.getStatus());
        }

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + request.getCarId()));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new RuntimeException("Car is not available. Status: " + car.getStatus());
        }

        if (rentalRepository.existsByCarIdAndEndTimeIsNull(car.getId())) {
            throw new RuntimeException("Car is already rented");
        }

        Rental rental = rentalMapper.toEntity(request, user, car);

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());
            if (services.size() != request.getServiceIds().size()) {
                throw new RuntimeException("Some services not found");
            }
            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    throw new RuntimeException("Service " + service.getName() + " is not available for this car");
                }
            }
            rental.setSelectedServices(services);
        }

        Rental savedRental = rentalRepository.save(rental);

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);

        return rentalMapper.toResponse(savedRental);
    }

    @Transactional
    public RentalResponse completeRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + id));

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new RuntimeException("Rental is not active. Status: " + rental.getStatus());
        }

        rental.setEndTime(LocalDateTime.now());
        rental.setStatus(RentalStatus.COMPLETED);

        Car car = rental.getCar();
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        Rental updatedRental = rentalRepository.save(rental);
        return rentalMapper.toResponse(updatedRental);
    }

    public List<RentalResponse> getAllRentalsWithNPlus1Problem() {

        return rentalRepository.findAll().stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RentalResponse> getAllRentalsWithDetails() {

        return rentalRepository.findAll().stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RentalResponse cancelRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + id));

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new RuntimeException("Rental is not active. Status: " + rental.getStatus());
        }

        rental.setStatus(RentalStatus.CANCELLED);
        rental.setEndTime(LocalDateTime.now());

        Car car = rental.getCar();
        car.setStatus(CarStatus.AVAILABLE);
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

    public List<RentalResponse> demonstrateNPlus1Problem() {
        System.out.println("========== ДЕМОНСТРАЦИЯ N+1 ПРОБЛЕМЫ ==========");
        // ВЫЗЫВАЕМ МЕДЛЕННЫЙ МЕТОД
        List<Rental> rentals = rentalRepository.findAllSlow();
        List<RentalResponse> responses = rentals.stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
        System.out.println("========== КОНЕЦ ДЕМОНСТРАЦИИ ==========");
        return responses;
    }

    public List<RentalResponse> demonstrateSolutionWithEntityGraph() {
        System.out.println("========== РЕШЕНИЕ N+1 ПРОБЛЕМЫ ==========");
        List<Rental> rentals = rentalRepository.findAll();
        List<RentalResponse> responses = rentals.stream()
                .map(rentalMapper::toResponse)
                .collect(Collectors.toList());
        System.out.println("========== КОНЕЦ РЕШЕНИЯ ==========");
        return responses;
    }

    public RentalResponse createRentalWithoutTransaction(RentalCreateRequest request) {
        log.info("=== ДЕМОНСТРАЦИЯ БЕЗ @Transactional ===");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new RuntimeException("Car is not available");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());

        Rental savedRental = rentalRepository.save(rental);
        log.info("Аренда сохранена в БД! ID: {}", savedRental.getId());

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        log.info("Статус машины обновлен на RENTED");

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            if (services.size() != request.getServiceIds().size()) {
                log.error("ОШИБКА: Не все сервисы найдены. Аренда {} уже в БД!", savedRental.getId());
                throw new RuntimeException("Some services not found");
            }

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    log.error("ОШИБКА: Сервис {} недоступен. Аренда {} уже в БД!",
                            service.getName(), savedRental.getId());
                    throw new RuntimeException("Service " + service.getName() + " is not available for this car");
                }
            }

            savedRental.setSelectedServices(services);
            rentalRepository.save(savedRental);
        }

        log.info("Аренда {} успешно создана (НО ЕСЛИ БЫЛА ОШИБКА - ОНА БЫ ОСТАЛАСЬ!)", savedRental.getId());
        return rentalMapper.toResponse(savedRental);
    }

    @Transactional
    public RentalResponse createRentalWithTransaction(RentalCreateRequest request) {
        log.info("=== ДЕМОНСТРАЦИЯ С @Transactional ===");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new RuntimeException("Car is not available");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());

        Rental savedRental = rentalRepository.save(rental);
        log.info("Аренда создана в памяти, но еще не закоммичена в БД");

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            if (services.size() != request.getServiceIds().size()) {
                log.error("ОШИБКА: Не все сервисы найдены. Транзакция откатится!");
                throw new RuntimeException("Some services not found");
            }

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    log.error("ОШИБКА: Сервис {} недоступен. Транзакция откатится!", service.getName());
                    throw new RuntimeException("Service " + service.getName() + " is not available for this car");
                }
            }
        }

        log.info("Транзакция успешно завершена. Аренда {} сохранена в БД", savedRental.getId());
        return rentalMapper.toResponse(savedRental);
    }

    private Rental createRentalEntity(RentalCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));
        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new RuntimeException("Car is not available. Status: " + car.getStatus());
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    throw new RuntimeException("Service " + service.getName() + " is not available for this car");
                }
            }
            rental.setSelectedServices(services);
        }

        return rental;
    }
}