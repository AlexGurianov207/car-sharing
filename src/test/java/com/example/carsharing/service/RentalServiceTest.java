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
        List<RentalCreateRequest> requests = List.of();
        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> rentalService.createRentalsBulkWithTransaction(requests));
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

        List<RentalCreateRequest> requests = List.of(first, second);
        assertThrows(DataIntegrityViolationException.class,
                () -> rentalService.createRentalsBulkWithoutTransaction(requests));

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
    void getUserRentals_whenEmpty_shouldReturnEmptyList() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(rentalRepository.findByUserId(1L)).thenReturn(List.of());

        List<RentalResponse> result = rentalService.getUserRentals(1L);

        assertEquals(0, result.size());
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

    @Test
    void createRental_whenUserHasActiveRental_shouldThrowConflict() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(true);

        assertThrows(com.example.carsharing.exception.ConflictException.class,
                () -> rentalService.createRental(request));
    }

    @Test
    void createRental_whenCarStatusNotAvailable_shouldThrowConflict() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setStatus(CarStatus.RENTED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(com.example.carsharing.exception.ConflictException.class,
                () -> rentalService.createRental(request));
    }

    @Test
    void createRental_whenValidWithServices_shouldAssignAndReturnResponse() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of(10L, 11L));
        User user = activeUser(1L);
        Car car = availableCar(1L);

        ExtraService gps = new ExtraService();
        gps.setId(10L);
        gps.setName("GPS");
        ExtraService childSeat = new ExtraService();
        childSeat.setId(11L);
        childSeat.setName("Child seat");
        car.setAvailableServices(List.of(gps, childSeat));

        Rental rental = baseRental(user, car);
        rental.setId(333L);
        RentalResponse expected = new RentalResponse();
        expected.setId(333L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(extraServiceRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(gps, childSeat));
        when(rentalMapper.toEntity(user, car)).thenReturn(rental);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalMapper.toResponse(rental)).thenReturn(expected);

        RentalResponse actual = rentalService.createRental(request);

        assertEquals(expected, actual);
        assertEquals(2, rental.getSelectedServices().size());
        verify(extraServiceRepository).findAllById(List.of(10L, 11L));
    }

    @Test
    void createRentalsBulkWithoutTransaction_whenAllValid_shouldReturnCounts() {
        RentalCreateRequest first = createRequest(1L, 1L, List.of());
        RentalCreateRequest second = createRequest(2L, 2L, List.of());

        User user1 = activeUser(1L);
        User user2 = activeUser(2L);
        Car car1 = availableCar(1L);
        Car car2 = availableCar(2L);

        Rental rental1 = baseRental(user1, car1);
        rental1.setId(801L);
        Rental rental2 = baseRental(user2, car2);
        rental2.setId(802L);

        RentalResponse response1 = new RentalResponse();
        response1.setId(801L);
        RentalResponse response2 = new RentalResponse();
        response2.setId(802L);

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

        BulkRentalResponse result = rentalService.createRentalsBulkWithoutTransaction(List.of(first, second));

        assertEquals(2, result.getRequestedCount());
        assertEquals(2, result.getCreatedCount());
        assertEquals(2, result.getRentals().size());
    }

    @Test
    void createRentalsBulk_shouldDelegateToTransactionalVersion() {
        RentalCreateRequest request = createRequest(1L, 1L, List.of());
        User user = activeUser(1L);
        Car car = availableCar(1L);
        Rental rental = baseRental(user, car);
        rental.setId(900L);
        RentalResponse response = new RentalResponse();
        response.setId(900L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalMapper.toEntity(user, car)).thenReturn(rental);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalMapper.toResponse(rental)).thenReturn(response);

        BulkRentalResponse result = rentalService.createRentalsBulk(List.of(request));

        assertEquals(1, result.getRequestedCount());
        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getRentals().size());
    }

    @Test
    void completeRental_whenDurationLessThanHour_shouldRoundUpToOneHourAndOneDay() {
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setPricePerHour(10.0);

        ExtraService service = new ExtraService();
        service.setName("GPS");
        service.setPricePerDay(3.0);

        Rental rental = baseRental(user, car);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(java.time.LocalDateTime.now().minusMinutes(10));
        rental.setSelectedServices(List.of(service));

        RentalResponse expected = new RentalResponse();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toResponse(rental)).thenReturn(expected);

        RentalResponse actual = rentalService.completeRental(1L);

        assertEquals(expected, actual);
        assertEquals(13.0, rental.getPayment().getAmount());
        assertEquals(10.0, rental.getPayment().getCarAmount());
        assertEquals(3.0, rental.getPayment().getServicesAmount());
        assertEquals("GPS", rental.getServiceNames());
    }

    @Test
    void completeRental_whenNoSelectedServices_shouldSkipServiceNamesAndServiceCost() {
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setPricePerHour(10.0);

        Rental rental = baseRental(user, car);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(java.time.LocalDateTime.now().minusHours(1));
        rental.setSelectedServices(List.of());

        RentalResponse expected = new RentalResponse();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toResponse(rental)).thenReturn(expected);

        RentalResponse actual = rentalService.completeRental(1L);

        assertEquals(expected, actual);
        assertEquals(10.0, rental.getPayment().getAmount());
        assertEquals(0.0, rental.getPayment().getServicesAmount());
    }

    @Test
    void searchRentalsJpql_whenBrandHasSpacesAndDifferentCase_shouldNormalizeAndUseCache() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        rental.setId(1001L);
        RentalResponse response = new RentalResponse();
        response.setId(1001L);

        when(rentalRepository.searchByFiltersJpqlNoPageFetch("toyota", false, -1L, false, RentalStatus.ACTIVE))
                .thenReturn(List.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(response);

        List<RentalResponse> first = rentalService.searchRentalsJpql("  TOYOTA  ", null, null);
        List<RentalResponse> second = rentalService.searchRentalsJpql("toyota", null, null);

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        verify(rentalRepository, org.mockito.Mockito.times(1))
                .searchByFiltersJpqlNoPageFetch("toyota", false, -1L, false, RentalStatus.ACTIVE);
    }

    @Test
    void mapIdsToResponses_whenIdsEmpty_shouldReturnEmptyList() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod("mapIdsToResponses", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<RentalResponse> result = (List<RentalResponse>) method.invoke(rentalService, List.of());

        assertEquals(List.of(), result);
    }

        @Test
    void searchRentalsNative_whenMinimalFilters_shouldMapLiveFields() {
        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getId()).thenReturn(1L);
        when(row.getUserId()).thenReturn(10L);
        when(row.getCarId()).thenReturn(20L);
        when(row.getStartTime()).thenReturn(java.time.LocalDateTime.now().minusHours(2));
        when(row.getEndTime()).thenReturn(null);
        when(row.getStatus()).thenReturn("ACTIVE");
        when(row.getUserFirstName()).thenReturn("Ivan");
        when(row.getUserLastName()).thenReturn("Petrov");
        when(row.getCarBrand()).thenReturn("Toyota");
        when(row.getCarModel()).thenReturn("Camry");
        when(row.getCarLicensePlate()).thenReturn("1234-AA");
        when(row.getSelectedServiceNames()).thenReturn("GPS; WiFi, Child Seat");

        when(rentalRepository.searchByFiltersNativeNoPage("", false, -1L, false, "ACTIVE"))
                .thenReturn(List.of(row));

        List<RentalResponse> result = rentalService.searchRentalsNative(null, null, null);

        assertEquals(1, result.size());
        assertEquals("Ivan Petrov", result.get(0).getUserFullName());
        assertEquals("Toyota Camry (1234-AA)", result.get(0).getCarInfo());
        assertEquals(List.of("GPS", "WiFi", "Child Seat"), result.get(0).getSelectedServices());
    }

        @Test
    void searchRentalsNative_whenCompleted_shouldPreferSnapshots() {
        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getId()).thenReturn(2L);
        when(row.getUserId()).thenReturn(11L);
        when(row.getCarId()).thenReturn(21L);
        when(row.getStartTime()).thenReturn(java.time.LocalDateTime.now().minusHours(3));
        when(row.getEndTime()).thenReturn(java.time.LocalDateTime.now().minusHours(1));
        when(row.getStatus()).thenReturn("COMPLETED");
        when(row.getUserFullNameSnapshot()).thenReturn("Snapshot User");
        when(row.getCarInfoSnapshot()).thenReturn("Snapshot Car");
        when(row.getServiceNamesSnapshot()).thenReturn("Snap1;Snap2");

        when(rentalRepository.searchByFiltersNativeNoPage("", false, -1L, false, "ACTIVE"))
                .thenReturn(List.of(row));

        List<RentalResponse> result = rentalService.searchRentalsNative("   ", null, null);

        assertEquals(1, result.size());
        assertEquals("Snapshot User", result.get(0).getUserFullName());
        assertEquals("Snapshot Car", result.get(0).getCarInfo());
        assertEquals(List.of("Snap1", "Snap2"), result.get(0).getSelectedServices());
    }

    @Test
    void searchRentalsJpql_whenUserAndStatusProvided_shouldPassFlags() {
        Rental rental = baseRental(activeUser(7L), availableCar(8L));
        RentalResponse mapped = new RentalResponse();
        mapped.setId(707L);

        when(rentalRepository.searchByFiltersJpqlNoPageFetch("bmw", true, 7L, true, RentalStatus.COMPLETED))
                .thenReturn(List.of(rental));
        when(rentalMapper.toResponse(rental)).thenReturn(mapped);

        List<RentalResponse> result = rentalService.searchRentalsJpql(" BMW ", 7L, RentalStatus.COMPLETED);

        assertEquals(1, result.size());
        verify(rentalRepository).searchByFiltersJpqlNoPageFetch("bmw", true, 7L, true, RentalStatus.COMPLETED);
    }

        @Test
    void searchRentalsNative_whenUserAndStatusProvided_shouldPassFlags() {
        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getId()).thenReturn(3L);
        when(row.getUserId()).thenReturn(7L);
        when(row.getCarId()).thenReturn(8L);
        when(row.getStartTime()).thenReturn(java.time.LocalDateTime.now().minusHours(1));
        when(row.getEndTime()).thenReturn(null);
        when(row.getStatus()).thenReturn("COMPLETED");
        when(row.getUserFullNameSnapshot()).thenReturn("A B");
        when(row.getCarInfoSnapshot()).thenReturn("BMW X5");
        when(row.getServiceNamesSnapshot()).thenReturn("GPS");

        when(rentalRepository.searchByFiltersNativeNoPage("bmw", true, 7L, true, "COMPLETED"))
                .thenReturn(List.of(row));

        List<RentalResponse> result = rentalService.searchRentalsNative("BMW", 7L, RentalStatus.COMPLETED);

        assertEquals(1, result.size());
        verify(rentalRepository).searchByFiltersNativeNoPage("bmw", true, 7L, true, "COMPLETED");
    }

    @Test
    void getRentalsPage_shouldMapRepositoryPage() {
        Rental rental = baseRental(activeUser(1L), availableCar(1L));
        RentalResponse mapped = new RentalResponse();
        mapped.setId(5000L);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(rentalRepository.findAllBy(pageable)).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(rental)));
        when(rentalMapper.toResponse(rental)).thenReturn(mapped);

        org.springframework.data.domain.Page<RentalResponse> page = rentalService.getRentalsPage(pageable);

        assertEquals(1, page.getContent().size());
        assertEquals(5000L, page.getContent().get(0).getId());
    }

    @Test
    void normalize_shouldHandleNullBlankAndTrimmedValues() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod("normalize", String.class);
        method.setAccessible(true);

        assertEquals("", method.invoke(rentalService, new Object[]{null}));
        assertEquals("", method.invoke(rentalService, "   "));
        assertEquals("toyota", method.invoke(rentalService, "  ToYoTa  "));
    }

    @Test
    void putToIndex_whenKeyAlreadyExists_shouldReturnPreviousAndUpdate() throws Exception {
        Class<?> queryTypeClass = java.util.Arrays.stream(RentalService.class.getDeclaredClasses())
                .filter(Class::isEnum)
                .findFirst()
                .orElseThrow();

        Object jpql = queryTypeClass.getEnumConstants()[0];

        Class<?> keyClass = java.util.Arrays.stream(RentalService.class.getDeclaredClasses())
                .filter(c -> !c.isEnum())
                .filter(c -> java.util.Arrays.stream(c.getDeclaredConstructors())
                        .anyMatch(ctor -> ctor.getParameterCount() == 7))
                .findFirst()
                .orElseThrow();

        java.lang.reflect.Constructor<?> ctor = keyClass.getDeclaredConstructor(
                String.class,
                Long.class,
                RentalStatus.class,
                int.class,
                int.class,
                String.class,
                queryTypeClass
        );
        ctor.setAccessible(true);
        Object key = ctor.newInstance("toyota", null, null, 0, -1, "LIST[startTime,DESC]", jpql);

        java.lang.reflect.Method putMethod = RentalService.class.getDeclaredMethod(
                "putToIndex",
                keyClass,
                org.springframework.data.domain.Page.class
        );
        putMethod.setAccessible(true);

        org.springframework.data.domain.Page<RentalResponse> first = new org.springframework.data.domain.PageImpl<>(List.of(new RentalResponse()));
        org.springframework.data.domain.Page<RentalResponse> second = new org.springframework.data.domain.PageImpl<>(List.of(new RentalResponse()));

        Object prev1 = putMethod.invoke(rentalService, key, first);
        Object prev2 = putMethod.invoke(rentalService, key, second);

        assertEquals(null, prev1);
        assertEquals(first, prev2);
    }

    @Test
    void completeRental_whenExactly24Hours_shouldUseOneDayForServices() {
        User user = activeUser(1L);
        Car car = availableCar(1L);
        car.setPricePerHour(2.0);

        ExtraService service = new ExtraService();
        service.setName("GPS");
        service.setPricePerDay(5.0);

        Rental rental = baseRental(user, car);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(java.time.LocalDateTime.now().minusHours(24));
        rental.setSelectedServices(List.of(service));

        RentalResponse expected = new RentalResponse();
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toResponse(rental)).thenReturn(expected);

        RentalResponse actual = rentalService.completeRental(1L);

        assertEquals(expected, actual);
        assertEquals(48.0, rental.getPayment().getCarAmount());
        assertEquals(5.0, rental.getPayment().getServicesAmount());
    }

    @Test
    void completeRental_whenSelectedServicesNull_shouldThrowNpe() {
        User user = activeUser(1L);
        Car car = availableCar(1L);

        Rental rental = baseRental(user, car);
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(java.time.LocalDateTime.now().minusHours(2));
        rental.setSelectedServices(null);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertThrows(NullPointerException.class, () -> rentalService.completeRental(1L));
    }

    @Test
    void rentalSearchCacheKey_equals_shouldCoverSelfNullAndFields() throws Exception {
        Class<?> queryTypeClass = java.util.Arrays.stream(RentalService.class.getDeclaredClasses())
                .filter(Class::isEnum)
                .findFirst()
                .orElseThrow();
        Object jpql = java.util.Arrays.stream(queryTypeClass.getEnumConstants())
                .filter(e -> e.toString().equals("JPQL"))
                .findFirst()
                .orElseThrow();

        Class<?> keyClass = java.util.Arrays.stream(RentalService.class.getDeclaredClasses())
                .filter(c -> !c.isEnum())
                .filter(c -> java.util.Arrays.stream(c.getDeclaredConstructors())
                        .anyMatch(ctor -> ctor.getParameterCount() == 7))
                .findFirst()
                .orElseThrow();

        java.lang.reflect.Constructor<?> ctor = keyClass.getDeclaredConstructor(
                String.class,
                Long.class,
                RentalStatus.class,
                int.class,
                int.class,
                String.class,
                queryTypeClass
        );
        ctor.setAccessible(true);

        Object key1 = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 0, -1, "LIST[startTime,DESC]", jpql);
        Object key2 = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 0, -1, "LIST[startTime,DESC]", jpql);

        assertEquals(true, key1.equals(key1));
        assertEquals(false, key1.equals(null));
        assertEquals(false, key1.equals("not-a-key"));
        assertEquals(true, key1.equals(key2));
    }

    @Test
    void splitServiceNames_shouldReturnEmptyForBlankAndFilterEmptyParts() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod("splitServiceNames", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> empty = (List<String>) method.invoke(rentalService, "   ");
        @SuppressWarnings("unchecked")
        List<String> parsed = (List<String>) method.invoke(rentalService, "GPS;; , WiFi; ");

        assertEquals(List.of(), empty);
        assertEquals(List.of("GPS", "WiFi"), parsed);
    }

    @Test
    void resolveUserFullName_shouldUseSnapshotOrLiveOrFallback() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod(
                "resolveUserFullName",
                RentalRepository.RentalNativeSearchProjection.class,
                boolean.class
        );
        method.setAccessible(true);

        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getUserFullNameSnapshot()).thenReturn("Snapshot Name");
        when(row.getUserFirstName()).thenReturn(null);
        when(row.getUserLastName()).thenReturn(null);

        String completed = (String) method.invoke(rentalService, row, true);
        String fallback = (String) method.invoke(rentalService, row, false);

        when(row.getUserFirstName()).thenReturn("Ivan");
        when(row.getUserLastName()).thenReturn("Petrov");
        String live = (String) method.invoke(rentalService, row, false);

        assertEquals("Snapshot Name", completed);
        assertEquals("Snapshot Name", fallback);
        assertEquals("Ivan Petrov", live);
    }

    @Test
    void resolveCarInfo_shouldUseSnapshotLiveOrFallback() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod(
                "resolveCarInfo",
                RentalRepository.RentalNativeSearchProjection.class,
                boolean.class
        );
        method.setAccessible(true);

        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getCarInfoSnapshot()).thenReturn("Snapshot Car");

        String completed = (String) method.invoke(rentalService, row, true);

        when(row.getCarBrand()).thenReturn("Toyota");
        when(row.getCarModel()).thenReturn("Camry");
        when(row.getCarLicensePlate()).thenReturn("1234-AA");
        String live = (String) method.invoke(rentalService, row, false);

        when(row.getCarBrand()).thenReturn(null);
        String fallback = (String) method.invoke(rentalService, row, false);

        assertEquals("Snapshot Car", completed);
        assertEquals("Toyota Camry (1234-AA)", live);
        assertEquals("Snapshot Car", fallback);
    }

    @Test
    void resolveSelectedServices_whenCompletedSnapshotBlank_shouldUseLiveList() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod(
                "resolveSelectedServices",
                RentalRepository.RentalNativeSearchProjection.class,
                boolean.class
        );
        method.setAccessible(true);

        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getServiceNamesSnapshot()).thenReturn("   ");
        when(row.getSelectedServiceNames()).thenReturn("GPS;WiFi");

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(rentalService, row, true);

        assertEquals(List.of("GPS", "WiFi"), result);
    }

    @Test
    void hasText_shouldHandleNullBlankAndValue() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod("hasText", String.class);
        method.setAccessible(true);

        assertEquals(false, method.invoke(rentalService, new Object[]{null}));
        assertEquals(false, method.invoke(rentalService, "   "));
        assertEquals(true, method.invoke(rentalService, "abc"));
    }

    @Test
    void resolveUserFullName_whenCompletedAndSnapshotBlank_shouldUseLiveName() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod(
                "resolveUserFullName",
                RentalRepository.RentalNativeSearchProjection.class,
                boolean.class
        );
        method.setAccessible(true);

        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getUserFullNameSnapshot()).thenReturn("   ");
        when(row.getUserFirstName()).thenReturn("John");
        when(row.getUserLastName()).thenReturn("Doe");

        String result = (String) method.invoke(rentalService, row, true);

        assertEquals("John Doe", result);
    }

    @Test
    void resolveCarInfo_whenCompletedAndSnapshotBlank_shouldCheckLivePartsAndFallback() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod(
                "resolveCarInfo",
                RentalRepository.RentalNativeSearchProjection.class,
                boolean.class
        );
        method.setAccessible(true);

        RentalRepository.RentalNativeSearchProjection row = org.mockito.Mockito.mock(
                RentalRepository.RentalNativeSearchProjection.class);
        when(row.getCarInfoSnapshot()).thenReturn("   ");

        when(row.getCarBrand()).thenReturn("Toyota");
        when(row.getCarModel()).thenReturn(null);
        String noModel = (String) method.invoke(rentalService, row, true);

        when(row.getCarModel()).thenReturn("Camry");
        when(row.getCarLicensePlate()).thenReturn(null);
        String noPlate = (String) method.invoke(rentalService, row, true);

        assertEquals("   ", noModel);
        assertEquals("   ", noPlate);
    }

    @Test
    void splitServiceNames_whenNull_shouldReturnEmptyList() throws Exception {
        java.lang.reflect.Method method = RentalService.class.getDeclaredMethod("splitServiceNames", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(rentalService, new Object[]{null});

        assertEquals(List.of(), result);
    }

    @Test
    void rentalSearchCacheKey_equals_shouldReturnFalseWhenEachFieldDiffers() throws Exception {
        Class<?> queryTypeClass = java.util.Arrays.stream(RentalService.class.getDeclaredClasses())
                .filter(Class::isEnum)
                .findFirst()
                .orElseThrow();

        Object jpql = java.util.Arrays.stream(queryTypeClass.getEnumConstants())
                .filter(e -> e.toString().equals("JPQL"))
                .findFirst()
                .orElseThrow();
        Object nativeType = java.util.Arrays.stream(queryTypeClass.getEnumConstants())
                .filter(e -> e.toString().equals("NATIVE"))
                .findFirst()
                .orElseThrow();

        Class<?> keyClass = java.util.Arrays.stream(RentalService.class.getDeclaredClasses())
                .filter(c -> !c.isEnum())
                .filter(c -> java.util.Arrays.stream(c.getDeclaredConstructors())
                        .anyMatch(ctor -> ctor.getParameterCount() == 7))
                .findFirst()
                .orElseThrow();

        java.lang.reflect.Constructor<?> ctor = keyClass.getDeclaredConstructor(
                String.class,
                Long.class,
                RentalStatus.class,
                int.class,
                int.class,
                String.class,
                queryTypeClass
        );
        ctor.setAccessible(true);

        Object base = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 0, -1, "LIST[startTime,DESC]", jpql);

        Object diffPage = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 1, -1, "LIST[startTime,DESC]", jpql);
        Object diffSize = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 0, 99, "LIST[startTime,DESC]", jpql);
        Object diffBrand = ctor.newInstance("bmw", 1L, RentalStatus.ACTIVE, 0, -1, "LIST[startTime,DESC]", jpql);
        Object diffUser = ctor.newInstance("toyota", 2L, RentalStatus.ACTIVE, 0, -1, "LIST[startTime,DESC]", jpql);
        Object diffStatus = ctor.newInstance("toyota", 1L, RentalStatus.COMPLETED, 0, -1, "LIST[startTime,DESC]", jpql);
        Object diffSort = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 0, -1, "OTHER", jpql);
        Object diffType = ctor.newInstance("toyota", 1L, RentalStatus.ACTIVE, 0, -1, "LIST[startTime,DESC]", nativeType);

        assertEquals(false, base.equals(diffPage));
        assertEquals(false, base.equals(diffSize));
        assertEquals(false, base.equals(diffBrand));
        assertEquals(false, base.equals(diffUser));
        assertEquals(false, base.equals(diffStatus));
        assertEquals(false, base.equals(diffSort));
        assertEquals(false, base.equals(diffType));
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
