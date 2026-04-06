package com.example.carsharing.service;

import com.example.carsharing.dto.BulkRentalResponse;
import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.repository.PaymentRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.RentalMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private ExtraServiceRepository extraServiceRepository;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RentalService rentalService;

    @Test
    void createRentalsBulk_whenRequestsNull_shouldThrow() {
        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalsBulkWithTransaction(null));
    }

    @Test
    void createRentalsBulk_whenRequestsEmpty_shouldThrow() {
        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalsBulkWithTransaction(List.of()));
    }

    @Test
    void createRentalsBulk_whenTooMany_shouldThrow() {
        List<RentalCreateRequest> requests = java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> createRequest(1L, 1L, List.of()))
                .toList();

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createRentalsBulkWithTransaction(requests));
    }

    @Test
    void createRentalsBulk_whenContainsNullItem_shouldThrow() {
        List<RentalCreateRequest> requests = java.util.Arrays.asList(createRequest(1L, 1L, List.of()), null);

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createRentalsBulkWithTransaction(requests));
    }

    @Test
    void createRental_whenUserMissing_shouldThrowNotFound() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> rentalService.createRental(request));
    }

    @Test
    void createRental_whenUserNotActive_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.BLOCKED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> rentalService.createRental(request));
    }

    @Test
    void createRental_whenCarAlreadyRented_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> rentalService.createRental(request));
    }

    @Test
    void createRental_whenServiceMissing_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L, 11L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental rental = baseRental(user, car);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalMapper.toEntity(user, car)).thenReturn(rental);

        ExtraService onlyOne = new ExtraService();
        onlyOne.setId(10L);
        when(extraServiceRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(onlyOne));

        assertThrows(java.util.NoSuchElementException.class, () -> rentalService.createRental(request));
    }

    @Test
    void createRental_whenServiceUnavailableForCar_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setAvailableServices(List.of());
        Rental rental = baseRental(user, car);

        ExtraService service = new ExtraService();
        service.setId(10L);
        service.setName("GPS");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalMapper.toEntity(user, car)).thenReturn(rental);
        when(extraServiceRepository.findAllById(List.of(10L))).thenReturn(List.of(service));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> rentalService.createRental(request));
    }

    @Test
    void createRentalsBulkWithoutTransaction_whenSecondFails_shouldKeepFirst() {
        RentalCreateRequest first = createRequest(1L, 1L, List.of());
        RentalCreateRequest second = createRequest(2L, 2L, List.of());

        User user1 = activeUser(1L);
        User user2 = activeUser(2L);
        Car car1 = availableCar(1L);
        Car car2 = availableCar(2L);

        Rental rental1 = baseRental(user1, car1);
        rental1.setId(100L);
        RentalResponse response1 = new RentalResponse();
        response1.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(2L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car1));
        when(carRepository.findById(2L)).thenReturn(Optional.of(car2));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(2L)).thenReturn(true);

        when(rentalMapper.toEntity(user1, car1)).thenReturn(rental1);
        when(rentalRepository.save(rental1)).thenReturn(rental1);
        when(carRepository.save(car1)).thenReturn(car1);
        when(rentalMapper.toResponse(rental1)).thenReturn(response1);

        assertThrows(DataIntegrityViolationException.class,
                () -> rentalService.createRentalsBulkWithoutTransaction(List.of(first, second)));

        verify(rentalRepository).save(rental1);
    }

    @Test
    void createRentalsBulkWithTransaction_whenAllValid_shouldReturnCounts() {
        RentalCreateRequest first = createRequest(1L, 1L, List.of());
        RentalCreateRequest second = createRequest(2L, 2L, List.of());

        User user1 = activeUser(1L);
        User user2 = activeUser(2L);
        Car car1 = availableCar(1L);
        Car car2 = availableCar(2L);

        Rental rental1 = baseRental(user1, car1);
        rental1.setId(101L);
        Rental rental2 = baseRental(user2, car2);
        rental2.setId(102L);

        RentalResponse response1 = new RentalResponse();
        response1.setId(101L);
        RentalResponse response2 = new RentalResponse();
        response2.setId(102L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(2L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car1));
        when(carRepository.findById(2L)).thenReturn(Optional.of(car2));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(2L)).thenReturn(false);

        when(rentalMapper.toEntity(user1, car1)).thenReturn(rental1);
        when(rentalMapper.toEntity(user2, car2)).thenReturn(rental2);
        when(rentalRepository.save(rental1)).thenReturn(rental1);
        when(rentalRepository.save(rental2)).thenReturn(rental2);
        when(carRepository.save(car1)).thenReturn(car1);
        when(carRepository.save(car2)).thenReturn(car2);
        when(rentalMapper.toResponse(rental1)).thenReturn(response1);
        when(rentalMapper.toResponse(rental2)).thenReturn(response2);

        BulkRentalResponse result = rentalService.createRentalsBulkWithTransaction(List.of(first, second));

        assertEquals(2, result.getRequestedCount());
        assertEquals(2, result.getCreatedCount());
        assertEquals(2, result.getRentals().size());
    }

    private RentalCreateRequest createRequest(Long userId, Long carId, List<Long> serviceIds) {
        RentalCreateRequest request = new RentalCreateRequest();
        request.setUserId(userId);
        request.setCarId(carId);
        request.setServiceIds(serviceIds);
        return request;
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstName("A");
        user.setLastName("B");
        return user;
    }

    private Car availableCar(Long id) {
        Car car = new Car();
        car.setId(id);
        car.setStatus(CarStatus.AVAILABLE);
        car.setBrand("Toyota");
        car.setModel("Camry");
        car.setLicensePlate("AA-" + id);
        car.setPricePerHour(10.0);
        return car;
    }

    private Rental baseRental(User user, Car car) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStatus(RentalStatus.ACTIVE);
        return rental;
    }
}
