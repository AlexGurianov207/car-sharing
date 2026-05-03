package com.example.carsharing.dto;

import lombok.Data;

@Data
public class AuthResponse {

    private String role;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
}
