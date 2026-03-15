package com.example.carsharing.service;

import com.example.carsharing.dto.ExtraServiceCreateRequest;
import com.example.carsharing.dto.ExtraServiceResponse;
import com.example.carsharing.exception.ConflictException;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.ServiceCategory;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.service.mapper.ExtraServiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExtraServiceService {

    private final ExtraServiceRepository extraServiceRepository;
    private final ExtraServiceMapper extraServiceMapper;

    private static final String SERVICE_NOT_FOUND_MESSAGE = "Service not found with id: ";

    public ExtraServiceResponse createService(ExtraServiceCreateRequest request) {
        if (extraServiceRepository.existsByName(request.getName())) {
            throw new ConflictException("Service with name " + request.getName() + " already exists");
        }

        ExtraService service = extraServiceMapper.toEntity(request);
        ExtraService savedService = extraServiceRepository.save(service);
        return extraServiceMapper.toResponse(savedService);
    }

    public ExtraServiceResponse getServiceById(Long id) {
        ExtraService service = extraServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(SERVICE_NOT_FOUND_MESSAGE + id));
        return extraServiceMapper.toResponse(service);
    }

    public List<ExtraServiceResponse> getAllServices(ServiceCategory category, Boolean onlyActive) {
        List<ExtraService> services;

        if (category != null) {
            services = extraServiceRepository.findByCategory(category);
        } else if (onlyActive != null && onlyActive) {
            services = extraServiceRepository.findByIsActiveTrue();
        } else {
            services = extraServiceRepository.findAll();
        }

        return services.stream()
                .map(extraServiceMapper::toResponse)
                .toList();
    }

    public ExtraServiceResponse updateService(Long id, ExtraServiceCreateRequest request) {
        ExtraService service = extraServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(SERVICE_NOT_FOUND_MESSAGE + id));

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPricePerDay(request.getPricePerDay());
        service.setCategory(request.getCategory());
        service.setIsActive(request.getIsActive());

        ExtraService updatedService = extraServiceRepository.save(service);
        return extraServiceMapper.toResponse(updatedService);
    }

    public void deactivateService(Long id) {
        ExtraService service = extraServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(SERVICE_NOT_FOUND_MESSAGE + id));
        service.setIsActive(false);
        extraServiceRepository.save(service);
    }

    public void deleteService(Long id) {
        if (!extraServiceRepository.existsById(id)) {
            throw new ConflictException(SERVICE_NOT_FOUND_MESSAGE + id);
        }
        extraServiceRepository.deleteById(id);
    }
}