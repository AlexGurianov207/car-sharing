package com.example.carsharing.repository;

import com.example.carsharing.model.User;
import com.example.carsharing.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByStatusNot(UserStatus status);

    boolean existsByEmail(String email);

    boolean existsByDriverLicense(String driverLicense);
}
