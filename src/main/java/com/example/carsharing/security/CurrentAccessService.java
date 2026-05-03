package com.example.carsharing.security;

import com.example.carsharing.exception.ConflictException;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserRole;
import com.example.carsharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAccessService {

    private final UserRepository userRepository;

    public AppPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            throw new AccessDeniedException("Authentication is required");
        }
        return principal;
    }

    public boolean isAdmin(Authentication authentication) {
        return requirePrincipal(authentication).getRole() == UserRole.ADMIN;
    }

    public Long requireCurrentUserId(Authentication authentication) {
        AppPrincipal principal = requirePrincipal(authentication);
        if (principal.getRole() == UserRole.ADMIN || principal.getUserId() == null) {
            throw new AccessDeniedException("Current account is not a customer");
        }
        return principal.getUserId();
    }

    public User requireCurrentUser(Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    public void requireAdminOrOwner(Authentication authentication, Long userId) {
        if (isAdmin(authentication)) {
            return;
        }
        Long currentUserId = requireCurrentUserId(authentication);
        if (!currentUserId.equals(userId)) {
            throw new AccessDeniedException("You can access only your own data");
        }
    }

    public void requireOwnRentalRequest(Authentication authentication, Long userId) {
        if (isAdmin(authentication)) {
            return;
        }
        Long currentUserId = requireCurrentUserId(authentication);
        if (!currentUserId.equals(userId)) {
            throw new ConflictException("A user can create rentals only for their own account");
        }
    }
}
