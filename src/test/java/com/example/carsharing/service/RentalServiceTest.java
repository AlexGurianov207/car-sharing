package com.example.carsharing.service;

import com.example.carsharing.dto.BulkRentalResponse;
import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.PaymentStatus;
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
import static org.mockito.Mockito.never;
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

    @Test
    void createRentalWithoutTransaction_whenServiceMissing_shouldThrowAfterPartialSaves() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L, 11L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental savedRental = baseRental(user, car);
        savedRental.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);

        ExtraService onlyOne = new ExtraService();
        onlyOne.setId(10L);
        when(extraServiceRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(onlyOne));

        assertThrows(java.util.NoSuchElementException.class,
                () -> rentalService.createRentalWithoutTransaction(request));

        verify(rentalRepository).save(org.mockito.ArgumentMatchers.any(Rental.class));
        verify(carRepository).save(car);
    }

    @Test
    void createRentalWithoutTransaction_whenServiceUnavailable_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setAvailableServices(List.of());
        Rental savedRental = baseRental(user, car);
        savedRental.setId(101L);

        ExtraService service = new ExtraService();
        service.setId(10L);
        service.setName("GPS");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(extraServiceRepository.findAllById(List.of(10L))).thenReturn(List.of(service));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalWithoutTransaction(request));
    }

    @Test
    void createRentalWithoutTransaction_whenValidWithServices_shouldReturnResponse() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        ExtraService service = new ExtraService();
        service.setId(10L);
        service.setName("GPS");
        car.setAvailableServices(List.of(service));

        Rental savedRental = baseRental(user, car);
        savedRental.setId(102L);
        RentalResponse expected = new RentalResponse();
        expected.setId(102L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(extraServiceRepository.findAllById(List.of(10L))).thenReturn(List.of(service));
        when(rentalMapper.toResponse(savedRental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRentalWithoutTransaction(request);

        assertEquals(expected, actual);
        verify(rentalRepository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any(Rental.class));
    }

    @Test
    void createRentalWithTransaction_whenServiceMissing_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L, 11L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental savedRental = baseRental(user, car);
        savedRental.setId(200L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);

        ExtraService onlyOne = new ExtraService();
        onlyOne.setId(10L);
        when(extraServiceRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(onlyOne));

        assertThrows(java.util.NoSuchElementException.class,
                () -> rentalService.createRentalWithTransaction(request));
    }

    @Test
    void createRentalWithTransaction_whenServiceUnavailable_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setAvailableServices(List.of());
        Rental savedRental = baseRental(user, car);
        savedRental.setId(201L);

        ExtraService service = new ExtraService();
        service.setId(10L);
        service.setName("GPS");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(extraServiceRepository.findAllById(List.of(10L))).thenReturn(List.of(service));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalWithTransaction(request));
    }

    @Test
    void createRentalWithTransaction_whenValidWithServices_shouldReturnResponse() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L));
        User user = activeUser(1L);
        Car car = availableCar(1L);
        ExtraService service = new ExtraService();
        service.setId(10L);
        service.setName("GPS");
        car.setAvailableServices(List.of(service));

        Rental savedRental = baseRental(user, car);
        savedRental.setId(202L);
        RentalResponse expected = new RentalResponse();
        expected.setId(202L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(extraServiceRepository.findAllById(List.of(10L))).thenReturn(List.of(service));
        when(rentalMapper.toResponse(savedRental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRentalWithTransaction(request);

        assertEquals(expected, actual);
        verify(rentalRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(Rental.class));
    }
    @Test
    void createRentalWithoutTransaction_whenCarNotAvailable_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setStatus(CarStatus.RENTED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalWithoutTransaction(request));
    }

    @Test
    void createRentalWithoutTransaction_whenNoServices_shouldReturnResponse() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental savedRental = baseRental(user, car);
        savedRental.setId(103L);
        RentalResponse expected = new RentalResponse();
        expected.setId(103L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalMapper.toResponse(savedRental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRentalWithoutTransaction(request);

        assertEquals(expected, actual);
        verify(extraServiceRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRentalWithTransaction_whenCarNotAvailable_shouldThrow() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setStatus(CarStatus.RENTED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalWithTransaction(request));
    }

    @Test
    void createRentalWithTransaction_whenNoServices_shouldReturnResponse() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental savedRental = baseRental(user, car);
        savedRental.setId(203L);
        RentalResponse expected = new RentalResponse();
        expected.setId(203L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalMapper.toResponse(savedRental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRentalWithTransaction(request);

        assertEquals(expected, actual);
        verify(extraServiceRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void demonstrateNPlus1Problem_shouldMapAll() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        RentalResponse response = new RentalResponse();
        when(rentalRepository.findAllSlow()).thenReturn(List.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(response);

        List<RentalResponse> result = rentalService.demonstrateNPlus1Problem();

        assertEquals(1, result.size());
    }

    @Test
    void demonstrateSolutionWithEntityGraph_shouldMapAll() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        RentalResponse response = new RentalResponse();
        when(rentalRepository.findAll()).thenReturn(List.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(response);

        List<RentalResponse> result = rentalService.demonstrateSolutionWithEntityGraph();

        assertEquals(1, result.size());
    }

    @Test
    void deleteRental_whenActive_shouldThrow() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        rental.setStatus(RentalStatus.ACTIVE);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> rentalService.deleteRental(1L));
    }

    @Test
    void deleteRental_whenPaymentLinked_shouldDetachAndDelete() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        rental.setStatus(RentalStatus.COMPLETED);
        com.example.carsharing.model.Payment payment = new com.example.carsharing.model.Payment();
        payment.setRental(rental);
        rental.setPayment(payment);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        rentalService.deleteRental(1L);

        assertEquals(null, payment.getRental());
        verify(paymentRepository).save(payment);
        verify(rentalRepository).delete(rental);
    }

    @Test
    void deleteRental_whenNoPayment_shouldDeleteOnlyRental() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        rental.setStatus(RentalStatus.COMPLETED);
        rental.setPayment(null);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        rentalService.deleteRental(1L);

        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(rentalRepository).delete(rental);
    }
    @Test
    void createRentalWithoutTransaction_whenServiceIdsNull_shouldSkipServicesCheck() {
        RentalCreateRequest request = createRequest(1L, 1L, null);
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental savedRental = baseRental(user, car);
        savedRental.setId(104L);
        RentalResponse expected = new RentalResponse();
        expected.setId(104L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalMapper.toResponse(savedRental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRentalWithoutTransaction(request);

        assertEquals(expected, actual);
        verify(extraServiceRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRentalWithTransaction_whenServiceIdsNull_shouldSkipServicesCheck() {
        RentalCreateRequest request = createRequest(1L, 1L, null);
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental savedRental = baseRental(user, car);
        savedRental.setId(204L);
        RentalResponse expected = new RentalResponse();
        expected.setId(204L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class))).thenReturn(savedRental);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalMapper.toResponse(savedRental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRentalWithTransaction(request);

        assertEquals(expected, actual);
        verify(extraServiceRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getRentalById_whenMissing_shouldThrow() {
        when(rentalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> rentalService.getRentalById(1L));
    }

    @Test
    void getRentalById_whenFound_shouldReturnMappedResponse() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        RentalResponse expected = new RentalResponse();
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(expected);

        RentalResponse actual = rentalService.getRentalById(1L);

        assertEquals(expected, actual);
    }

    @Test
    void getUserRentals_whenUserMissing_shouldThrow() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(java.util.NoSuchElementException.class, () -> rentalService.getUserRentals(1L));
    }

    @Test
    void getUserRentals_whenEmpty_shouldThrow() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(rentalRepository.findByUserId(1L)).thenReturn(List.of());

        assertThrows(java.util.NoSuchElementException.class, () -> rentalService.getUserRentals(1L));
    }

    @Test
    void getUserRentals_whenFound_shouldMapAll() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        RentalResponse response = new RentalResponse();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(rentalRepository.findByUserId(1L)).thenReturn(List.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(response);

        List<RentalResponse> result = rentalService.getUserRentals(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getCarRentals_whenCarMissing_shouldThrow() {
        when(carRepository.existsById(1L)).thenReturn(false);

        assertThrows(java.util.NoSuchElementException.class, () -> rentalService.getCarRentals(1L));
    }

    @Test
    void getCarRentals_whenEmpty_shouldThrow() {
        when(carRepository.existsById(1L)).thenReturn(true);
        when(rentalRepository.findByCarId(1L)).thenReturn(List.of());

        assertThrows(java.util.NoSuchElementException.class, () -> rentalService.getCarRentals(1L));
    }

    @Test
    void getCarRentals_whenFound_shouldMapAll() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        RentalResponse response = new RentalResponse();
        when(carRepository.existsById(1L)).thenReturn(true);
        when(rentalRepository.findByCarId(1L)).thenReturn(List.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(response);

        List<RentalResponse> result = rentalService.getCarRentals(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getActiveRentals_whenEmpty_shouldThrow() {
        when(rentalRepository.findActiveRentalIds()).thenReturn(List.of());

        assertThrows(java.util.NoSuchElementException.class, () -> rentalService.getActiveRentals());
    }

    @Test
    void getActiveRentals_whenFound_shouldMapInIdOrder() {
        Rental r1 = baseRental(activeUser(1L), availableCar(1L));
        r1.setId(10L);
        Rental r2 = baseRental(activeUser(2L), availableCar(2L));
        r2.setId(20L);
        RentalResponse resp1 = new RentalResponse();
        resp1.setId(10L);
        RentalResponse resp2 = new RentalResponse();
        resp2.setId(20L);

        when(rentalRepository.findActiveRentalIds()).thenReturn(List.of(10L, 20L));
        when(rentalRepository.findAllWithDetailsByIdIn(List.of(10L, 20L))).thenReturn(List.of(r1, r2));
        when(rentalMapper.toResponse(r1)).thenReturn(resp1);
        when(rentalMapper.toResponse(r2)).thenReturn(resp2);

        List<RentalResponse> result = rentalService.getActiveRentals();

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(20L, result.get(1).getId());
    }

    @Test
    void completeRental_whenMissing_shouldThrow() {
        when(rentalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> rentalService.completeRental(1L));
    }

    @Test
    void completeRental_whenNotActive_shouldThrow() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        rental.setStatus(RentalStatus.COMPLETED);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> rentalService.completeRental(1L));
    }

    @Test
    void completeRental_whenBeforeStart_shouldThrow() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(java.time.LocalDateTime.now().plusHours(1));
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> rentalService.completeRental(1L));
    }

    @Test
    void completeRental_whenValid_shouldCompleteAndReturnResponse() {
        User user = activeUser(1L);
        Car car = availableCar(1L);
        ExtraService service = new ExtraService();
        service.setName("GPS");
        service.setPricePerDay(5.0);

        Rental rental = baseRental(user, car);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(java.time.LocalDateTime.now().minusHours(2));
        rental.setSelectedServices(List.of(service));

        RentalResponse expected = new RentalResponse();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toResponse(rental)).thenReturn(expected);

        RentalResponse actual = rentalService.completeRental(1L);

        assertEquals(expected, actual);
        assertEquals(RentalStatus.COMPLETED, rental.getStatus());
        assertEquals(CarStatus.AVAILABLE, car.getStatus());
        assertEquals(PaymentStatus.COMPLETED, rental.getPayment().getStatus());
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
