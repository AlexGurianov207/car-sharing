package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request for creating or updating a user")
public class UserCreateRequest {

    @Schema(description = "User first name", example = "Anna")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Schema(description = "User last name", example = "Ivanova")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Schema(description = "User email", example = "anna@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User phone number", example = "+375291112233")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @Schema(description = "Driver license number", example = "AB123456")
    @NotBlank(message = "Driver license is required")
    @Size(min = 5, max = 20, message = "Driver license must be between 5 and 20 characters")
    private String driverLicense;
}
