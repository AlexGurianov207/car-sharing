package com.example.carsharing.controller;

import com.example.carsharing.dto.UserCreateRequest;
import com.example.carsharing.dto.UserResponse;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.security.CurrentAccessService;
import com.example.carsharing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "Operations for user management")
public class UserController {

    private final UserService userService;
    private final CurrentAccessService currentAccessService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserResponse getCurrentUser(Authentication authentication) {
        return userService.getUserById(currentAccessService.requireCurrentUserId(authentication));
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public UserResponse getUserById(@PathVariable @Positive(message = "User ID must be positive") Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/by-email")
    @Operation(summary = "Get user by email")
    public UserResponse getUserByEmail(@RequestParam @Email(message = "Invalid email format") String email) {
        return userService.getUserByEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public UserResponse updateUser(
            @PathVariable @Positive(message = "User ID must be positive") Long id,
            @Valid @RequestBody UserCreateRequest request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status")
    public void updateUserStatus(
            @PathVariable @Positive(message = "User ID must be positive") Long id,
            @RequestParam UserStatus status) {
        userService.updateUserStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user")
    public void deleteUser(@PathVariable @Positive(message = "User ID must be positive") Long id) {
        userService.deleteUser(id);
    }
}
