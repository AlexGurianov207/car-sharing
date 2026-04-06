package com.example.carsharing.service;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.service.mapper.CarMapper;
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
class CarServiceTest {

    @Mock
    private CarRepository carRepository;
    @Mock
    private CarMapper carMapper;
    @Mock
    private ExtraServiceRepository extraServiceRepository;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private RentalService rentalService;

    @InjectMocks
    private CarService carService;

    @Test
    void createCar_whenLicenseExists_shouldThrow() {
        CarCreateRequest request = createRequest();
        when(carRepository.existsByLicensePlate("1234AB-7")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> carService.createCar(request));
    }

    @Test
    void createCar_whenValid_shouldSaveAndInvalidateCache() {
        CarCreateRequest request = createRequest();
        Car entity = baseCar(CarStatus.AVAILABLE);
        CarResponse expected = new CarResponse();
        when(carRepository.existsByLicensePlate("1234AB-7")).thenReturn(false);
        when(carMapper.toEntity(request)).thenReturn(entity);
        when(carRepository.save(entity)).thenReturn(entity);
        when(carMapper.toResponse(entity)).thenReturn(expected);

        CarResponse actual = carService.createCar(request);

        assertEquals(expected, actual);
        verify(rentalService).invalidateSearchIndex();
    }

    @Test
    void findById_whenFound_shouldReturnMappedResponse() {
        Car car = baseCar(CarStatus.AVAILABLE);
        CarResponse expected = new CarResponse();
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carMapper.toResponse(car)).thenReturn(expected);

        CarResponse actual = carService.findById(1L);

        assertEquals(expected, actual);
    }

    @Test
    void findById_whenMissing_shouldThrowNotFound() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> carService.findById(1L));
    }

    @Test
    void findByLicensePlate_whenFound_shouldReturnMappedResponse() {
        Car car = baseCar(CarStatus.AVAILABLE);
        CarResponse expected = new CarResponse();
        when(carRepository.findByLicensePlate("1234AB-7")).thenReturn(Optional.of(car));
        when(carMapper.toResponse(car)).thenReturn(expected);

        CarResponse actual = carService.findByLicensePlate("1234AB-7");

        assertEquals(expected, actual);
    }

    @Test
    void findByLicensePlate_whenMissing_shouldThrowNotFound() {
        when(carRepository.findByLicensePlate("1234AB-7")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> carService.findByLicensePlate("1234AB-7"));
    }

    @Test
    void findAll_whenStatusProvided_shouldUseFilteredRepository() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findByStatus(CarStatus.AVAILABLE)).thenReturn(List.of(car));
        when(carMapper.toResponse(car)).thenReturn(new CarResponse());

        List<CarResponse> result = carService.findAll(CarStatus.AVAILABLE);

        assertEquals(1, result.size());
        verify(carRepository).findByStatus(CarStatus.AVAILABLE);
    }

    @Test
    void findAll_whenStatusNull_shouldUseFindAll() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findAll()).thenReturn(List.of(car));
        when(carMapper.toResponse(car)).thenReturn(new CarResponse());

        List<CarResponse> result = carService.findAll(null);

        assertEquals(1, result.size());
        verify(carRepository).findAll();
    }

    @Test
    void findByMaxPrice_shouldMapAll() {
        Car car = baseCar(CarStatus.AVAILABLE);
        CarResponse response = new CarResponse();
        when(carRepository.findByPricePerHourLessThanEqual(15.0)).thenReturn(List.of(car));
        when(carMapper.toResponse(car)).thenReturn(response);

        List<CarResponse> result = carService.findByMaxPrice(15.0);

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void findByBrandAndModel_shouldMapAll() {
        Car car = baseCar(CarStatus.AVAILABLE);
        CarResponse response = new CarResponse();
        when(carRepository.findByBrandAndModel("Toyota", "Camry")).thenReturn(List.of(car));
        when(carMapper.toResponse(car)).thenReturn(response);

        List<CarResponse> result = carService.findByBrandAndModel("Toyota", "Camry");

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void updateCar_whenDeleted_shouldThrow() {
        Car existing = baseCar(CarStatus.DELETED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateCar(1L, createRequest()));
    }

    @Test
    void updateCar_whenRentedWithActiveRental_shouldThrow() {
        Car existing = baseCar(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(true);

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateCar(1L, createRequest()));
    }

    @Test
    void updateCar_whenLicenseChangedToTaken_shouldThrow() {
        Car existing = baseCar(CarStatus.AVAILABLE);
        existing.setLicensePlate("OLD-PLATE");
        CarCreateRequest request = createRequest();
        when(carRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(carRepository.existsByLicensePlate("1234AB-7")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> carService.updateCar(1L, request));
    }

    @Test
    void updateCar_whenSameLicense_shouldSaveWithoutDuplicateCheck() {
        Car existing = baseCar(CarStatus.AVAILABLE);
        CarCreateRequest request = createRequest();
        CarResponse expected = new CarResponse();
        when(carRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(carRepository.save(existing)).thenReturn(existing);
        when(carMapper.toResponse(existing)).thenReturn(expected);

        CarResponse actual = carService.updateCar(1L, request);

        assertEquals(expected, actual);
        verify(carRepository, never()).existsByLicensePlate("1234AB-7");
    }

    @Test
    void updateCar_whenValid_shouldSave() {
        Car existing = baseCar(CarStatus.AVAILABLE);
        CarCreateRequest request = createRequest();
        CarResponse expected = new CarResponse();
        when(carRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(carRepository.save(existing)).thenReturn(existing);
        when(carMapper.toResponse(existing)).thenReturn(expected);

        CarResponse actual = carService.updateCar(1L, request);

        assertEquals(expected, actual);
        verify(rentalService).invalidateSearchIndex();
    }

    @Test
    void deleteCar_whenDeleted_shouldReturn() {
        Car car = baseCar(CarStatus.DELETED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        carService.deleteCar(1L);

        verify(carRepository, never()).deleteById(1L);
    }

    @Test
    void deleteCar_whenRented_shouldThrow() {
        Car car = baseCar(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> carService.deleteCar(1L));
    }

    @Test
    void deleteCar_whenHasRentalHistory_shouldSoftDelete() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarId(1L)).thenReturn(true);

        carService.deleteCar(1L);

        assertEquals(CarStatus.DELETED, car.getStatus());
        verify(carRepository).save(car);
        verify(rentalService).invalidateSearchIndex();
    }

    @Test
    void deleteCar_whenNoRentalHistory_shouldHardDelete() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarId(1L)).thenReturn(false);

        carService.deleteCar(1L);

        verify(carRepository).deleteById(1L);
        verify(rentalService).invalidateSearchIndex();
    }

    @Test
    void updateAvailableServices_whenDeletedCar_shouldThrow() {
        Car car = baseCar(CarStatus.DELETED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateAvailableServices(1L, List.of(1L)));
    }

    @Test
    void updateAvailableServices_whenRentedWithActiveRental_shouldThrow() {
        Car car = baseCar(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndEndTimeIsNull(1L)).thenReturn(true);

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateAvailableServices(1L, List.of(1L)));
    }

    @Test
    void updateAvailableServices_whenServiceIdsNull_shouldThrow() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateAvailableServices(1L, null));
    }

    @Test
    void updateAvailableServices_whenServiceIdsEmpty_shouldThrow() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateAvailableServices(1L, List.of()));
    }

    @Test
    void updateAvailableServices_whenServiceIdsContainNull_shouldThrow() {
        Car car = baseCar(CarStatus.AVAILABLE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateAvailableServices(1L, java.util.Arrays.asList(1L, null)));
    }

    @Test
    void updateAvailableServices_whenServiceMissing_shouldThrow() {
        Car car = baseCar(CarStatus.AVAILABLE);
        ExtraService service = new ExtraService();
        service.setId(1L);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(extraServiceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(service));

        assertThrows(DataIntegrityViolationException.class,
                () -> carService.updateAvailableServices(1L, List.of(1L, 2L)));
    }

    @Test
    void updateAvailableServices_whenContainsInactive_shouldThrow() {
        Car car = baseCar(CarStatus.AVAILABLE);
        ExtraService active = new ExtraService();
        active.setId(1L);
        active.setIsActive(true);
        ExtraService inactive = new ExtraService();
        inactive.setId(2L);
        inactive.setName("GPS");
        inactive.setIsActive(false);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(extraServiceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(active, inactive));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> carService.updateAvailableServices(1L, List.of(1L, 2L)));
    }

    @Test
    void updateAvailableServices_whenValid_shouldSave() {
        Car car = baseCar(CarStatus.AVAILABLE);
        ExtraService s1 = new ExtraService();
        s1.setId(1L);
        s1.setIsActive(true);
        ExtraService s2 = new ExtraService();
        s2.setId(2L);
        s2.setIsActive(true);
        CarResponse expected = new CarResponse();

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(extraServiceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(s1, s2));
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toResponse(car)).thenReturn(expected);

        CarResponse actual = carService.updateAvailableServices(1L, List.of(1L, 2L));

        assertEquals(expected, actual);
        assertEquals(2, car.getAvailableServices().size());
        verify(rentalService).invalidateSearchIndex();
    }

    private CarCreateRequest createRequest() {
        CarCreateRequest request = new CarCreateRequest();
        request.setLicensePlate("1234AB-7");
        request.setBrand("Toyota");
        request.setModel("Camry");
        request.setYear(2020);
        request.setPricePerHour(10.0);
        return request;
    }

    private Car baseCar(CarStatus status) {
        Car car = new Car();
        car.setId(1L);
        car.setStatus(status);
        car.setLicensePlate("1234AB-7");
        car.setBrand("Toyota");
        car.setModel("Camry");
        car.setYear(2020);
        car.setPricePerHour(10.0);
        return car;
    }
}
