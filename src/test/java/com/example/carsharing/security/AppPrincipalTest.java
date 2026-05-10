package com.example.carsharing.security;

import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.model.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPrincipalTest {

    @Test
    void fromUser_whenUserIsActiveAndRoleMissing_shouldUseUserRoleAndEnableAccount() {
        User user = new User();
        user.setId(7L);
        user.setEmail("user@test.com");
        user.setPasswordHash("encoded");
        user.setFirstName("Ann");
        user.setLastName("Stone");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(null);

        AppPrincipal principal = AppPrincipal.fromUser(user);

        assertEquals(7L, principal.getUserId());
        assertEquals("user@test.com", principal.getUsername());
        assertEquals("encoded", principal.getPassword());
        assertEquals("Ann", principal.getFirstName());
        assertEquals("Stone", principal.getLastName());
        assertEquals(UserRole.USER, principal.getRole());
        assertTrue(principal.isEnabled());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isCredentialsNonExpired());
        assertEquals("ROLE_USER", principal.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void fromUser_whenUserBlocked_shouldDisableLockCheck() {
        User user = new User();
        user.setEmail("blocked@test.com");
        user.setStatus(UserStatus.BLOCKED);
        user.setRole(UserRole.USER);

        AppPrincipal principal = AppPrincipal.fromUser(user);

        assertFalse(principal.isEnabled());
        assertFalse(principal.isAccountNonLocked());
    }

    @Test
    void admin_shouldCreateEnabledAdminPrincipal() {
        AppPrincipal principal = AppPrincipal.admin("admin", "encoded-admin");

        assertEquals("admin", principal.getUsername());
        assertEquals("encoded-admin", principal.getPassword());
        assertEquals(UserRole.ADMIN, principal.getRole());
        assertEquals(UserStatus.ACTIVE, principal.getStatus());
        assertTrue(principal.isEnabled());
        assertEquals("ROLE_ADMIN", principal.getAuthorities().iterator().next().getAuthority());
    }
}
