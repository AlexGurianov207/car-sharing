package com.example.carsharing.service;

import com.example.carsharing.dto.TariffCreateRequest;
import com.example.carsharing.dto.TariffResponse;
import com.example.carsharing.model.Tariff;
import com.example.carsharing.repository.TariffRepository;
import com.example.carsharing.service.mapper.TariffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TariffService {

    private final TariffRepository tariffRepository;
    private final TariffMapper tariffMapper;

    public TariffResponse createTariff(TariffCreateRequest request) {
        if (tariffRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tariff with name " + request.getName() + " already exists");
        }

        Tariff tariff = tariffMapper.toEntity(request);
        Tariff savedTariff = tariffRepository.save(tariff);
        return tariffMapper.toResponse(savedTariff);
    }

    public TariffResponse getTariffById(Long id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + id));
        return tariffMapper.toResponse(tariff);
    }

    public List<TariffResponse> getAllTariffs(boolean onlyActive) {
        List<Tariff> tariffs;
        if (onlyActive) {
            tariffs = tariffRepository.findByIsActiveTrue();
        } else {
            tariffs = tariffRepository.findAll();
        }

        return tariffs.stream()
                .map(tariffMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TariffResponse updateTariff(Long id, TariffCreateRequest request) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + id));

        tariff.setName(request.getName());
        tariff.setDescription(request.getDescription());
        tariff.setPricePerHour(request.getPricePerHour());
        tariff.setPricePerDay(request.getPricePerDay());
        tariff.setMinRentalHours(request.getMinRentalHours());
        tariff.setMaxRentalDays(request.getMaxRentalDays());
        tariff.setIsActive(request.getIsActive());

        Tariff updatedTariff = tariffRepository.save(tariff);
        return tariffMapper.toResponse(updatedTariff);
    }

    public void deactivateTariff(Long id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + id));
        tariff.setIsActive(false);
        tariffRepository.save(tariff);
    }
}