package com.example.carsharing.security;

import com.example.carsharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarsharingUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.admin.login:admin}")
    private String adminLogin;

    @Value("${app.auth.admin.password:Admin123!}")
    private String adminPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (adminLogin.equalsIgnoreCase(username)) {
            return AppPrincipal.admin(adminLogin, passwordEncoder.encode(adminPassword));
        }

        return userRepository.findByEmail(username)
                .map(AppPrincipal::fromUser)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
