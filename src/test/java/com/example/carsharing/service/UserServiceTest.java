package com.example.carsharing.service;

import com.example.carsharing.dto.UserCreateRequest;
import com.example.carsharing.dto.UserResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_whenEmailExists_shouldThrow() {
        UserCreateRequest request = createRequest();
        when(userRepository.existsByEmail("u@test.com")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_whenDriverLicenseExists_shouldThrow() {
        UserCreateRequest request = createRequest();
        when(userRepository.existsByEmail("u@test.com")).thenReturn(false);
        when(userRepository.existsByDriverLicense("AB1234567")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_whenValid_shouldReturnResponse() {
        UserCreateRequest request = createRequest();
        User entity = new User();
        User saved = new User();
        UserResponse expected = new UserResponse();
        when(userRepository.existsByEmail("u@test.com")).thenReturn(false);
        when(userRepository.existsByDriverLicense("AB1234567")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        UserResponse actual = userService.createUser(request);

        assertEquals(expected, actual);
    }

    @Test
    void getUserById_whenMissing_shouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getUserByEmail_whenMissing_shouldThrow() {
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserByEmail("u@test.com"));
    }

    @Test
    void getAllUsers_shouldMapAll() {
        User u1 = new User();
        UserResponse r1 = new UserResponse();
        when(userRepository.findAll()).thenReturn(List.of(u1));
        when(userMapper.toResponse(u1)).thenReturn(r1);

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    void updateUser_whenDeleted_shouldThrow() {
        User existing = new User();
        existing.setStatus(UserStatus.DELETED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.updateUser(1L, createRequest()));
    }

    @Test
    void updateUser_whenActive_shouldSave() {
        User existing = new User();
        existing.setStatus(UserStatus.ACTIVE);
        UserCreateRequest request = createRequest();
        UserResponse expected = new UserResponse();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toResponse(existing)).thenReturn(expected);

        UserResponse actual = userService.updateUser(1L, request);

        assertEquals(expected, actual);
        assertEquals("Name", existing.getFirstName());
    }

    @Test
    void updateUserStatus_whenStatusNull_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(1L, null));
    }

    @Test
    void updateUserStatus_whenUserDeleted_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.DELETED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> userService.updateUserStatus(1L, UserStatus.ACTIVE));
    }

    @Test
    void updateUserStatus_whenBlockingWithActiveRental_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(true);

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> userService.updateUserStatus(1L, UserStatus.BLOCKED));
    }

    @Test
    void updateUserStatus_whenValid_shouldSave() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);

        userService.updateUserStatus(1L, UserStatus.BLOCKED);

        assertEquals(UserStatus.BLOCKED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_whenAlreadyDeleted_shouldReturn() {
        User user = new User();
        user.setStatus(UserStatus.DELETED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository, never()).delete(user);
    }

    @Test
    void deleteUser_whenHasActiveRentals_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(true);

        assertThrows(InvalidDataAccessApiUsageException.class, () -> userService.deleteUser(1L));
    }

    @Test
    void deleteUser_whenHasRentalHistory_shouldSoftDelete() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalRepository.existsByUserId(1L)).thenReturn(true);

        userService.deleteUser(1L);

        assertEquals(UserStatus.DELETED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_whenNoRentals_shouldHardDelete() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rentalRepository.existsByUserIdAndEndTimeIsNull(1L)).thenReturn(false);
        when(rentalRepository.existsByUserId(1L)).thenReturn(false);

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    private UserCreateRequest createRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setFirstName("Name");
        request.setLastName("Surname");
        request.setEmail("u@test.com");
        request.setPhoneNumber("+375291111111");
        request.setDriverLicense("AB1234567");
        return request;
    }
}
