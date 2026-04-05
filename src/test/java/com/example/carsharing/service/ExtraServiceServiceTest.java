package com.example.carsharing.service;

import com.example.carsharing.dto.ExtraServiceCreateRequest;
import com.example.carsharing.dto.ExtraServiceResponse;
import com.example.carsharing.exception.ConflictException;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.ServiceCategory;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.service.mapper.ExtraServiceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtraServiceServiceTest {

    @Mock
    private ExtraServiceRepository extraServiceRepository;
    @Mock
    private ExtraServiceMapper extraServiceMapper;
    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private ExtraServiceService extraServiceService;

    @Test
    void createService_whenNameExists_shouldThrowConflict() {
        ExtraServiceCreateRequest request = createRequest();
        when(extraServiceRepository.existsByName("GPS")).thenReturn(true);

        assertThrows(ConflictException.class, () -> extraServiceService.createService(request));

        verify(extraServiceRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createService_whenValid_shouldReturnResponse() {
        ExtraServiceCreateRequest request = createRequest();
        ExtraService entity = new ExtraService();
        ExtraService saved = new ExtraService();
        ExtraServiceResponse expected = new ExtraServiceResponse();

        when(extraServiceRepository.existsByName("GPS")).thenReturn(false);
        when(extraServiceMapper.toEntity(request)).thenReturn(entity);
        when(extraServiceRepository.save(entity)).thenReturn(saved);
        when(extraServiceMapper.toResponse(saved)).thenReturn(expected);

        ExtraServiceResponse actual = extraServiceService.createService(request);

        assertEquals(expected, actual);
    }

    @Test
    void getServiceById_whenMissing_shouldThrowNotFound() {
        when(extraServiceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> extraServiceService.getServiceById(1L));
    }

    @Test
    void getAllServices_whenCategoryProvided_shouldUseCategoryFilter() {
        ExtraService service = new ExtraService();
        ExtraServiceResponse response = new ExtraServiceResponse();
        when(extraServiceRepository.findByCategory(ServiceCategory.COMFORT)).thenReturn(List.of(service));
        when(extraServiceMapper.toResponse(service)).thenReturn(response);

        List<ExtraServiceResponse> result = extraServiceService.getAllServices(ServiceCategory.COMFORT, null);

        assertEquals(1, result.size());
        verify(extraServiceRepository).findByCategory(ServiceCategory.COMFORT);
    }

    @Test
    void getAllServices_whenOnlyActiveTrue_shouldUseActiveFilter() {
        ExtraService service = new ExtraService();
        ExtraServiceResponse response = new ExtraServiceResponse();
        when(extraServiceRepository.findByIsActiveTrue()).thenReturn(List.of(service));
        when(extraServiceMapper.toResponse(service)).thenReturn(response);

        List<ExtraServiceResponse> result = extraServiceService.getAllServices(null, true);

        assertEquals(1, result.size());
        verify(extraServiceRepository).findByIsActiveTrue();
    }

    @Test
    void getAllServices_whenNoFilters_shouldReturnAll() {
        ExtraService service = new ExtraService();
        ExtraServiceResponse response = new ExtraServiceResponse();
        when(extraServiceRepository.findAll()).thenReturn(List.of(service));
        when(extraServiceMapper.toResponse(service)).thenReturn(response);

        List<ExtraServiceResponse> result = extraServiceService.getAllServices(null, false);

        assertEquals(1, result.size());
        verify(extraServiceRepository).findAll();
    }

    @Test
    void updateService_whenActive_shouldThrow() {
        ExtraService existing = new ExtraService();
        existing.setIsActive(true);
        when(extraServiceRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> extraServiceService.updateService(1L, createRequest()));

        verify(extraServiceRepository, never()).save(existing);
    }

    @Test
    void updateService_whenInactive_shouldUpdateAndSave() {
        ExtraService existing = new ExtraService();
        existing.setIsActive(false);
        ExtraServiceResponse expected = new ExtraServiceResponse();
        ExtraServiceCreateRequest request = createRequest();

        when(extraServiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(extraServiceRepository.save(existing)).thenReturn(existing);
        when(extraServiceMapper.toResponse(existing)).thenReturn(expected);

        ExtraServiceResponse actual = extraServiceService.updateService(1L, request);

        assertEquals(expected, actual);
        assertEquals("GPS", existing.getName());
        assertEquals(ServiceCategory.COMFORT, existing.getCategory());
    }

    @Test
    void updateServiceStatus_whenActivate_shouldSetTrue() {
        ExtraService service = new ExtraService();
        service.setIsActive(false);
        when(extraServiceRepository.findById(1L)).thenReturn(Optional.of(service));

        extraServiceService.updateServiceStatus(1L, true);

        assertEquals(true, service.getIsActive());
        verify(extraServiceRepository).save(service);
    }

    @Test
    void updateServiceStatus_whenDeactivateAndAttachedToCars_shouldThrow() {
        ExtraService service = new ExtraService();
        service.setIsActive(true);
        Car car = new Car();
        car.setLicensePlate("1234AB-7");
        when(extraServiceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(carRepository.findByAvailableServicesId(1L)).thenReturn(List.of(car));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> extraServiceService.updateServiceStatus(1L, false));

        verify(extraServiceRepository, never()).save(service);
    }

    @Test
    void deleteService_whenMissing_shouldThrowNotFound() {
        when(extraServiceRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> extraServiceService.deleteService(1L));
    }

    @Test
    void deleteService_whenExists_shouldDelete() {
        when(extraServiceRepository.existsById(1L)).thenReturn(true);

        extraServiceService.deleteService(1L);

        verify(extraServiceRepository).deleteById(1L);
    }

    private ExtraServiceCreateRequest createRequest() {
        ExtraServiceCreateRequest request = new ExtraServiceCreateRequest();
        request.setName("GPS");
        request.setDescription("Navigation");
        request.setPricePerDay(10.0);
        request.setCategory(ServiceCategory.COMFORT);
        request.setIsActive(false);
        return request;
    }
}
