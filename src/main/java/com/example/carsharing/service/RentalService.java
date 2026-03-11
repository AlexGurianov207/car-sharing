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
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active. Status: " + user.getStatus());
        }

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + request.getCarId()));

        if (!"AVAILABLE".equals(car.getStatus())) {
            throw new RuntimeException("Car is not available. Status: " + car.getStatus());
        }

        if (rentalRepository.existsByCarIdAndEndTimeIsNull(car.getId())) {
            throw new RuntimeException("Car is already rented");
        }

        Rental rental = rentalMapper.toEntity(request, user, car);

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());
            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    throw new RuntimeException("Service " + service.getName() + " is not available for this car");
                }
            }
            rental.setSelectedServices(services);
        }

        Rental savedRental = rentalRepository.save(rental);

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

        rental.setEndTime(LocalDateTime.now());
        rental.setStatus("COMPLETED");

        Car car = rental.getCar();
        car.setStatus("AVAILABLE");
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

        if (!"ACTIVE".equals(rental.getStatus())) {
            throw new RuntimeException("Rental is not active. Status: " + rental.getStatus());
        }

        rental.setStatus("CANCELLED");
        rental.setEndTime(LocalDateTime.now());

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

    public List<RentalResponse> demonstrateNPlus1Problem() {
        System.out.println("========== ДЕМОНСТРАЦИЯ N+1 ПРОБЛЕМЫ ==========");
        List<Rental> rentals = rentalRepository.findAll();
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

    public void createTwoRentalsWithoutTransaction(RentalCreateRequest request1, RentalCreateRequest request2) {
        System.out.println("========== БЕЗ @TRANSACTIONAL ==========");

        Rental rental1 = createRentalEntity(request1);
        rentalRepository.save(rental1);
        System.out.println("Первая аренда сохранена, ID: " + rental1.getId());

        Rental rental2 = createRentalEntity(request2);
        if (request2.getCarId() == null) {
            throw new RuntimeException("ОШИБКА! Вторая аренда не сохранится, но первая уже в БД!");
        }
        rentalRepository.save(rental2);
    }

    @Transactional
    public void createTwoRentalsWithTransaction(RentalCreateRequest request1, RentalCreateRequest request2) {
        System.out.println("========== С @TRANSACTIONAL ==========");

        Rental rental1 = createRentalEntity(request1);
        rentalRepository.save(rental1);
        System.out.println("Первая аренда создана, но еще не закоммичена");

        Rental rental2 = createRentalEntity(request2);
        if (request2.getCarId() == null) {
            throw new RuntimeException("ОШИБКА! Транзакция откатится, обе аренды НЕ сохранятся!");
        }
        rentalRepository.save(rental2);

        System.out.println("Обе аренды успешно сохранены");
    }

    private Rental createRentalEntity(RentalCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        return rental;
    }
}