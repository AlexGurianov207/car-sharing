package com.example.carsharing.repository;

import com.example.carsharing.model.Rental;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByUserId(Long userId);

    List<Rental> findByCarId(Long carId);

    List<Rental> findByStatus(String status);

    // Находим все активные аренды (без endTime)
    List<Rental> findByEndTimeIsNull();

    // Находим аренды пользователя с определенным статусом
    List<Rental> findByUserIdAndStatus(Long userId, String status);

    // Проверяем, арендована ли машина в данный момент
    boolean existsByCarIdAndEndTimeIsNull(Long carId);

    // ✅ ИСПРАВЛЕНО: метод должен называться findAll (это встроенный метод)
    // а @EntityGraph просто добавляем к нему
    @EntityGraph(attributePaths = {"user", "car", "selectedServices"})
    List<Rental> findAll();

    // Сложный запрос с JPQL (Java Persistence Query Language)
    @Query("SELECT r FROM Rental r WHERE r.car.id = :carId AND " +
            "((r.startTime BETWEEN :start AND :end) OR " +
            "(r.endTime BETWEEN :start AND :end) OR " +
            "(r.startTime <= :start AND (r.endTime IS NULL OR r.endTime >= :end)))")
    List<Rental> findOverlappingRentals(
            @Param("carId") Long carId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}