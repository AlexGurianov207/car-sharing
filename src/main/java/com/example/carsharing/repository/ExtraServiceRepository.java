package com.example.carsharing.repository;

import com.example.carsharing.model.ExtraService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExtraServiceRepository extends JpaRepository<ExtraService, Long> {

    List<ExtraService> findByCategory(String category);

    List<ExtraService> findByIsActiveTrue();

    List<ExtraService> findByPricePerDayLessThanEqual(Double maxPrice);

    boolean existsByName(String name);
}