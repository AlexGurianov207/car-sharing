package com.example.carsharing.repository;

import com.example.carsharing.model.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    List<Tariff> findByIsActiveTrue();

    List<Tariff> findByPricePerHourLessThanEqual(Double maxPrice);

    boolean existsByName(String name);
}