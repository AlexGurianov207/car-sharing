package com.example.carsharing.service;

import com.example.carsharing.dto.UserCreateRequest;
import com.example.carsharing.dto.UserResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RentalRepository rentalRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";

    @Value("${app.auth.user.initial-credential}")
    private String initialUserCredential;

    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("User with email " + request.getEmail() + " already exists");
        }

        if (userRepository.existsByDriverLicense(request.getDriverLicense())) {
            throw new DataIntegrityViolationException("User with driver license "
                    + request.getDriverLicense() + " already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(initialUserCredential));
        user.setRole(UserRole.USER);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + id));
        return userMapper.toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse updateUser(Long id, UserCreateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + id));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new InvalidDataAccessApiUsageException("Cannot update deleted user");
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
                .orElseThrow(() -> new NotFoundException("User not found"));

        Set<UserStatus> allowedStatuses = Set.of(UserStatus.ACTIVE, UserStatus.BLOCKED);
        UserStatus validatedStatus = Optional.ofNullable(newStatus)
                .filter(allowedStatuses::contains)
                .orElseThrow(() -> new IllegalArgumentException("Status can only be changed to ACTIVE or BLOCKED"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new InvalidDataAccessApiUsageException("Cannot change status of deleted user");
        }

        if (validatedStatus == UserStatus.BLOCKED && rentalRepository.existsByUserIdAndEndTimeIsNull(id)) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot block user with active rental");
        }

        user.setStatus(validatedStatus);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + id));

        if (user.getStatus() == UserStatus.DELETED) {
            return;
        }

        if (rentalRepository.existsByUserIdAndEndTimeIsNull(id)) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot delete user with active rentals. Complete or cancel rentals first.");
        }

        if (rentalRepository.existsByUserId(id)) {
            user.setStatus(UserStatus.DELETED);
            userRepository.save(user);
            return;
        }

        userRepository.delete(user);
    }
}
