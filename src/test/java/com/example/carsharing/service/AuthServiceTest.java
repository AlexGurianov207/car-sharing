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
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_whenEmailExists_shouldThrow() {
        AuthRegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request, new MockHttpServletRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenDriverLicenseExists_shouldThrow() {
        AuthRegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByDriverLicense("DL12345")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request, new MockHttpServletRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenValid_shouldCreateUserAndAuthenticate() {
        AuthRegisterRequest request = registerRequest();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        User savedUser = user(5L, "new@test.com");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByDriverLicense("DL12345")).thenReturn(false);
        when(passwordEncoder.encode("register-credential")).thenReturn("encoded-register");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authenticationManager.authenticate(any())).thenReturn(authenticated(savedUser));

        AuthResponse response = authService.register(request, httpRequest);

        assertEquals(5L, response.getUserId());
        assertEquals("new@test.com", response.getEmail());
        assertEquals("USER", response.getRole());
        HttpSession session = httpRequest.getSession(false);
        assertNotNull(session);
        assertNotNull(session.getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void login_whenValid_shouldAuthenticateAndReturnResponse() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setLogin("new@test.com");
        request.setPassword("login-credential");
        User user = user(5L, "new@test.com");
        when(authenticationManager.authenticate(any())).thenReturn(authenticated(user));

        AuthResponse response = authService.login(request, new MockHttpServletRequest());

        assertEquals(5L, response.getUserId());
        assertEquals("new@test.com", response.getEmail());
    }

    @Test
    void login_whenBadCredentials_shouldThrowConflict() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setLogin("bad@test.com");
        request.setPassword("bad-credential");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(ConflictException.class, () -> authService.login(request, new MockHttpServletRequest()));
    }

    @Test
    void login_whenDisabled_shouldThrowConflict() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setLogin("disabled@test.com");
        request.setPassword("disabled-credential");
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThrows(ConflictException.class, () -> authService.login(request, new MockHttpServletRequest()));
    }

    @Test
    void getCurrentUser_whenPrincipalPresent_shouldReturnResponse() {
        User user = user(5L, "new@test.com");

        AuthResponse response = authService.getCurrentUser(authenticated(user));

        assertEquals(5L, response.getUserId());
        assertEquals("new@test.com", response.getEmail());
    }

    @Test
    void getCurrentUser_whenAuthenticationInvalid_shouldThrow() {
        Authentication authentication = new TestingAuthenticationToken("raw-user", null);

        assertThrows(ConflictException.class, () -> authService.getCurrentUser(authentication));
    }

    private AuthRegisterRequest registerRequest() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new@test.com");
        request.setPhoneNumber("+375291234567");
        request.setDriverLicense("DL12345");
        request.setPassword("register-credential");
        return request;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName("New");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash("encoded");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        return user;
    }

    private Authentication authenticated(User user) {
        AppPrincipal principal = AppPrincipal.fromUser(user);
        return new TestingAuthenticationToken(principal, null, "ROLE_" + principal.getRole().name());
    }
}
