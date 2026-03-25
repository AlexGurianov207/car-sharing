package com.example.carsharing.repository;

import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @EntityGraph(attributePaths = {"user", "car", "selectedServices", "payment"})
    @Query("SELECT DISTINCT r FROM Rental r WHERE r.id IN :ids")
    List<Rental> findAllWithDetailsByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT r.id FROM Rental r")
    Page<Long> findAllIds(Pageable pageable);

    @Query("""
            SELECT r.id
            FROM Rental r
            WHERE r.endTime IS NULL
            ORDER BY r.startTime DESC
            """)
    List<Long> findActiveRentalIds();

    @Query("""
            SELECT r.id
            FROM Rental r
            LEFT JOIN r.car c
            LEFT JOIN r.user u
            WHERE (:carBrand = '' OR LOWER(c.brand) = :carBrand)
              AND (:hasUserId = false OR u.id = :userId)
              AND (:hasStatus = false OR r.status = :status)
            ORDER BY r.startTime DESC
            """)
    List<Long> searchByFiltersJpqlNoPage(
            @Param("carBrand") String carBrand,
            @Param("hasUserId") boolean hasUserId,
            @Param("userId") Long userId,
            @Param("hasStatus") boolean hasStatus,
            @Param("status") RentalStatus status
    );

    @Query(value = """
            SELECT r.id
            FROM rentals r
            LEFT JOIN cars c ON r.car_id = c.id
            LEFT JOIN users u ON r.user_id = u.id
            WHERE (:carBrand = '' OR LOWER(c.brand) = :carBrand)
              AND (:hasUserId = false OR u.id = :userId)
              AND (:hasStatus = false OR r.status = :status)
            ORDER BY r.start_time DESC
            """,
            nativeQuery = true)
    List<Long> searchByFiltersNativeNoPage(
            @Param("carBrand") String carBrand,
            @Param("hasUserId") boolean hasUserId,
            @Param("userId") Long userId,
            @Param("hasStatus") boolean hasStatus,
            @Param("status") String status
    );
}
