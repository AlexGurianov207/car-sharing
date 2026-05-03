package com.example.carsharing.security;

import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.model.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AppPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final UserRole role;
    private final UserStatus status;
    private final boolean enabled;

    private AppPrincipal(
            Long userId,
            String username,
            String password,
            String firstName,
            String lastName,
            UserRole role,
            UserStatus status,
            boolean enabled
    ) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = status;
        this.enabled = enabled;
    }

    public static AppPrincipal fromUser(User user) {
        boolean active = user.getStatus() == UserStatus.ACTIVE;
        UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();
        return new AppPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                role,
                user.getStatus(),
                active
        );
    }

    public static AppPrincipal admin(String login, String encodedPassword) {
        return new AppPrincipal(
                null,
                login,
                encodedPassword,
                "System",
                "Administrator",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                true
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
