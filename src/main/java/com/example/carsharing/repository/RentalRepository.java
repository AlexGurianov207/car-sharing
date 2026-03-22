package com.example.carsharing.repository;

import com.example.carsharing.model.Rental;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByUserId(Long userId);

    List<Rental> findByCarId(Long carId);

    List<Rental> findByEndTimeIsNull();

    boolean existsByCarIdAndEndTimeIsNull(Long carId);

    boolean existsByUserIdAndEndTimeIsNull(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByCarId(Long carId);

    @Query("SELECT r FROM Rental r")
    List<Rental> findAllSlow();

    @EntityGraph(attributePaths = {"user", "car", "selectedServices", "payment"})
    List<Rental> findAll();
}
