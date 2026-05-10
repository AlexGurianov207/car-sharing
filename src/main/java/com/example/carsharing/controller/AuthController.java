package com.example.carsharing.controller;

import com.example.carsharing.dto.AuthLoginRequest;
import com.example.carsharing.dto.AuthRegisterRequest;
import com.example.carsharing.dto.AuthResponse;
import com.example.carsharing.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new user")
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "Login by email or staff login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Compatibility alias for staff login")
    public AuthResponse loginAdmin(@Valid @RequestBody AuthLoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated account")
    public AuthResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }

    @GetMapping("/csrf")
    @Operation(summary = "Get CSRF token")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout current account")
    public void logout(HttpServletRequest httpRequest, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(httpRequest, null, authentication);
    }
}
