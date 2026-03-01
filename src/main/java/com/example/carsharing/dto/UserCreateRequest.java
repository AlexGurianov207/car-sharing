package com.example.carsharing.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UserCreateRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @NotBlank(message = "Driver license is required")
    @Size(min = 5, max = 20, message = "Driver license must be between 5 and 20 characters")
    private String driverLicense;
}