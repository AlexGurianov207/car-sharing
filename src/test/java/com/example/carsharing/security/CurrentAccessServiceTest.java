package com.example.carsharing.security;

import com.example.carsharing.exception.ConflictException;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentAccessService service;

    @Test
    void requirePrincipal_whenAuthenticationMissing_shouldThrow() {
        assertThrows(AccessDeniedException.class, () -> service.requirePrincipal(null));
    }

    @Test
    void requirePrincipal_whenPrincipalHasWrongType_shouldThrow() {
        Authentication authentication = new TestingAuthenticationToken("raw-user", null);

        assertThrows(AccessDeniedException.class, () -> service.requirePrincipal(authentication));
    }

    @Test
    void isAdmin_whenAdminPrincipal_shouldReturnTrue() {
        assertEquals(true, service.isAdmin(auth(adminPrincipal())));
    }

    @Test
    void requireCurrentUserId_whenCustomer_shouldReturnId() {
        assertEquals(10L, service.requireCurrentUserId(auth(userPrincipal(10L))));
    }

    @Test
    void requireCurrentUserId_whenAdmin_shouldThrow() {
        assertThrows(AccessDeniedException.class, () -> service.requireCurrentUserId(auth(adminPrincipal())));
    }

    @Test
    void requireCurrentUser_whenUserExists_shouldReturnUser() {
        User user = new User();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertEquals(user, service.requireCurrentUser(auth(userPrincipal(10L))));
    }

    @Test
    void requireCurrentUser_whenMissing_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.requireCurrentUser(auth(userPrincipal(10L))));
    }

    @Test
    void requireAdminOrOwner_whenOwnerOrAdmin_shouldPass() {
        assertDoesNotThrow(() -> service.requireAdminOrOwner(auth(userPrincipal(10L)), 10L));
        assertDoesNotThrow(() -> service.requireAdminOrOwner(auth(adminPrincipal()), 77L));
    }

    @Test
    void requireAdminOrOwner_whenAnotherUser_shouldThrow() {
        assertThrows(AccessDeniedException.class,
                () -> service.requireAdminOrOwner(auth(userPrincipal(10L)), 11L));
    }

    @Test
    void requireOwnRentalRequest_whenAnotherUser_shouldThrowConflict() {
        assertThrows(ConflictException.class,
                () -> service.requireOwnRentalRequest(auth(userPrincipal(10L)), 11L));
    }

    @Test
    void requireOwnRentalRequest_whenOwnerOrAdmin_shouldPass() {
        assertDoesNotThrow(() -> service.requireOwnRentalRequest(auth(userPrincipal(10L)), 10L));
        assertDoesNotThrow(() -> service.requireOwnRentalRequest(auth(adminPrincipal()), 77L));
    }

    private Authentication auth(AppPrincipal principal) {
        return new TestingAuthenticationToken(principal, null, "ROLE_" + principal.getRole().name());
    }

    private AppPrincipal userPrincipal(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        return AppPrincipal.fromUser(user);
    }

    private AppPrincipal adminPrincipal() {
        return AppPrincipal.admin("admin", "encoded");
    }
}
