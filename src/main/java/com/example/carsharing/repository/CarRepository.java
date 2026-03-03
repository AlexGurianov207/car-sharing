package com.example.carsharing.repository;

import com.example.carsharing.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    // Spring Data JPA сам реализует эти методы на основе их названий!

    Optional<Car> findByLicensePlate(String licensePlate);

    List<Car> findByStatus(String status);

    List<Car> findByBrandAndModel(String brand, String model);

    List<Car> findByYearBetween(Integer startYear, Integer endYear);

    List<Car> findByPricePerHourLessThanEqual(Double maxPrice);

    boolean existsByLicensePlate(String licensePlate);
}