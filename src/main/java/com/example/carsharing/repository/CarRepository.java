package com.example.carsharing.repository;

import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    Optional<Car> findByLicensePlate(String licensePlate);

    List<Car> findByStatus(CarStatus status);

    List<Car> findByBrandAndModel(String brand, String model);

    List<Car> findByPricePerHourLessThanEqual(Double maxPrice);

    boolean existsByLicensePlate(String licensePlate);
}