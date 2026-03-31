package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "User response")
public class UserResponse {
    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "First name", example = "Anna")
    private String firstName;

    @Schema(description = "Last name", example = "Ivanova")
    private String lastName;

    @Schema(description = "Email", example = "anna@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+375291112233")
    private String phoneNumber;

    @Schema(description = "Driver license", example = "AB123456")
    private String driverLicense;

    @Schema(description = "Registration date-time")
    private LocalDateTime registrationDate;

    @Schema(description = "User status", example = "ACTIVE")
    private String status;
}
