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

    interface RentalNativeSearchProjection {
        Long getId();
        Long getUserId();
        String getUserFirstName();
        String getUserLastName();
        String getUserFullNameSnapshot();
        Long getCarId();
        String getCarBrand();
        String getCarModel();
        String getCarLicensePlate();
        String getCarInfoSnapshot();
        java.time.LocalDateTime getStartTime();
        java.time.LocalDateTime getEndTime();
        String getStatus();
        String getServiceNamesSnapshot();
        String getSelectedServiceNames();
    }

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

    @EntityGraph(attributePaths = {"user", "car", "selectedServices", "payment"})
    Page<Rental> findAllBy(Pageable pageable);

    @Query("""
            SELECT r.id
            FROM Rental r
            WHERE r.endTime IS NULL
            ORDER BY r.startTime DESC
            """)
    List<Long> findActiveRentalIds();

    @Query("""
            SELECT DISTINCT r
            FROM Rental r
            LEFT JOIN FETCH r.car c
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.payment p
            LEFT JOIN FETCH r.selectedServices s
            WHERE (:carBrand = '' OR LOWER(c.brand) = :carBrand)
              AND (:hasUserId = false OR u.id = :userId)
              AND (:hasStatus = false OR r.status = :status)
            ORDER BY r.startTime DESC
            """)
    List<Rental> searchByFiltersJpqlNoPageFetch(
            @Param("carBrand") String carBrand,
            @Param("hasUserId") boolean hasUserId,
            @Param("userId") Long userId,
            @Param("hasStatus") boolean hasStatus,
            @Param("status") RentalStatus status
    );

    @Query(value = """
            SELECT
                r.id AS id,
                r.user_id AS userId,
                u.first_name AS userFirstName,
                u.last_name AS userLastName,
                r.user_full_name AS userFullNameSnapshot,
                r.car_id AS carId,
                c.brand AS carBrand,
                c.model AS carModel,
                c.license_plate AS carLicensePlate,
                r.car_info AS carInfoSnapshot,
                r.start_time AS startTime,
                r.end_time AS endTime,
                r.status AS status,
                r.service_names AS serviceNamesSnapshot,
                COALESCE(
                    string_agg(es.name, ',' ORDER BY es.name)
                    FILTER (WHERE es.id IS NOT NULL),
                    ''
                ) AS selectedServiceNames
            FROM rentals r
            LEFT JOIN cars c ON r.car_id = c.id
            LEFT JOIN users u ON r.user_id = u.id
            LEFT JOIN rental_selected_services rss ON r.id = rss.rental_id
            LEFT JOIN extra_services es ON rss.service_id = es.id
            WHERE (:carBrand = '' OR LOWER(c.brand) = :carBrand)
              AND (:hasUserId = false OR u.id = :userId)
              AND (:hasStatus = false OR r.status = :status)
            GROUP BY
                r.id, r.user_id, u.first_name, u.last_name, r.user_full_name,
                r.car_id, c.brand, c.model, c.license_plate, r.car_info,
                r.start_time, r.end_time, r.status, r.service_names
            ORDER BY r.start_time DESC
            """,
            nativeQuery = true)
    List<RentalNativeSearchProjection> searchByFiltersNativeNoPage(
            @Param("carBrand") String carBrand,
            @Param("hasUserId") boolean hasUserId,
            @Param("userId") Long userId,
            @Param("hasStatus") boolean hasStatus,
            @Param("status") String status
    );
}
