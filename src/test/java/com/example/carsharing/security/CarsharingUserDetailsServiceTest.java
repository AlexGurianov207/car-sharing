package com.example.carsharing.security;

import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarsharingUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CarsharingUserDetailsService service;

    @Test
    void loadUserByUsername_whenAdminLoginDifferentCase_shouldReturnAdmin() {
        ReflectionTestUtils.setField(service, "adminLogin", "admin");
        ReflectionTestUtils.setField(service, "adminPassword", "admin-credential");
        when(passwordEncoder.encode("admin-credential")).thenReturn("encoded-admin");

        UserDetails details = service.loadUserByUsername("ADMIN");

        assertEquals("admin", details.getUsername());
        assertEquals("encoded-admin", details.getPassword());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
        verify(userRepository, never()).findByEmail("ADMIN");
    }

    @Test
    void loadUserByUsername_whenUserExists_shouldReturnUserPrincipal() {
        ReflectionTestUtils.setField(service, "adminLogin", "admin");
        User user = new User();
        user.setId(3L);
        user.setEmail("user@test.com");
        user.setPasswordHash("encoded-user");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@test.com");

        assertEquals("user@test.com", details.getUsername());
        assertEquals("encoded-user", details.getPassword());
    }

    @Test
    void loadUserByUsername_whenUserMissing_shouldThrow() {
        ReflectionTestUtils.setField(service, "adminLogin", "admin");
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@test.com"));
    }
}
