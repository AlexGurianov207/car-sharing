package com.example.carsharing.service;

import com.example.carsharing.dto.AuthLoginRequest;
import com.example.carsharing.dto.AuthRegisterRequest;
import com.example.carsharing.dto.AuthResponse;
import com.example.carsharing.exception.ConflictException;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(AuthRegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        if (userRepository.existsByDriverLicense(request.getDriverLicense())) {
            throw new ConflictException("User with driver license " + request.getDriverLicense() + " already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDriverLicense(request.getDriverLicense());
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return authenticateAndBuildResponse(savedUser.getEmail(), request.getPassword(), httpRequest);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthLoginRequest request, HttpServletRequest httpRequest) {
        return authenticateAndBuildResponse(request.getLogin(), request.getPassword(), httpRequest);
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            throw new ConflictException("Authentication is required");
        }
        return toAuthResponse(principal);
    }

    private AuthResponse authenticateAndBuildResponse(String login, String password, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, password)
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", context);
            return toAuthResponse((AppPrincipal) authentication.getPrincipal());
        } catch (BadCredentialsException ex) {
            throw new ConflictException("Invalid login or password");
        } catch (DisabledException | LockedException ex) {
            throw new ConflictException("User account is not active");
        }
    }

    private AuthResponse toAuthResponse(AppPrincipal principal) {
        AuthResponse response = new AuthResponse();
        response.setRole(principal.getRole().name());
        response.setUserId(principal.getUserId());
        response.setFirstName(principal.getFirstName());
        response.setLastName(principal.getLastName());
        response.setEmail(principal.getUsername());
        return response;
    }
}
