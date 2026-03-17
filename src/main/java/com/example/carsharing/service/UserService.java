package com.example.carsharing.service;

import com.example.carsharing.dto.UserCreateRequest;
import com.example.carsharing.dto.UserResponse;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RentalRepository rentalRepository;

    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";

    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("User with email " + request.getEmail() + " already exists");
        }

        if (userRepository.existsByDriverLicense(request.getDriverLicense())) {
            throw new DataIntegrityViolationException("User with driver license "
                    + request.getDriverLicense() + " already exists");
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE + id));
        return userMapper.toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse updateUser(Long id, UserCreateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE + id));

        List<Rental> userRentals = rentalRepository.findByUserId(id);
        if (!userRentals.isEmpty()) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot update user with rental history. User has " +
                            userRentals.size() + " past rentals.");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setDriverLicense(request.getDriverLicense());

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    public void updateUserStatus(Long id, UserStatus newStatus) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (newStatus != UserStatus.ACTIVE && newStatus != UserStatus.BLOCKED) {
            throw new IllegalArgumentException("Status can only be changed to ACTIVE or BLOCKED");
        }

        if (newStatus == UserStatus.BLOCKED && rentalRepository.existsByUserIdAndEndTimeIsNull(id)) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot block user with active rental");
        }

        user.setStatus(newStatus);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + id));

        boolean hasActiveRentals = rentalRepository.existsByUserIdAndEndTimeIsNull(id);

        if (hasActiveRentals) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot delete user with active rentals. Complete or cancel rentals first.");
        }

        userRepository.delete(user);
    }
}