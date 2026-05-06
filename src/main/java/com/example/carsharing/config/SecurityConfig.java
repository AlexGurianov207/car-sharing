package com.example.carsharing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/app.js",
                                "/app.css",
                                "/favicon.ico",
                                "/webjars/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/admin/login", "/api/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cars/**", "/api/services/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cars/**", "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cars/**", "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cars/**", "/api/services/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/payments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/payments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/payments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/rentals/me", "/api/rentals/*", "/api/rentals/user/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/rentals").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rentals/search/**", "/api/rentals/search/paged", "/api/rentals/car/**", "/api/rentals/active")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/rentals/bulk/**", "/api/rentals/demo/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/rentals/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/rentals/**").hasRole("ADMIN")
                        .requestMatchers("/api/lab/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .rememberMe(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
