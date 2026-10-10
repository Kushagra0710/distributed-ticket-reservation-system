package com.ticketing.ticket_reservation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ticketing.ticket_reservation.dto.AdminUserRequest;
import com.ticketing.ticket_reservation.entity.Role;
import com.ticketing.ticket_reservation.entity.User;
import com.ticketing.ticket_reservation.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void registerOwnerGeneratesAndHashesInitialPassword() {
        AdminUserRequest request = new AdminUserRequest();
        request.setFullName("Venue Owner");
        request.setEmail("owner@example.com");
        request.setPhoneNumber("9876543210");
        request.setRole(Role.OWNER);

        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        userService.registerAdmin(request);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertEquals(12, generatedPassword.length());
        assertTrue(generatedPassword.chars().anyMatch(Character::isUpperCase));
        assertTrue(generatedPassword.chars().anyMatch(Character::isLowerCase));
        assertTrue(generatedPassword.chars().anyMatch(Character::isDigit));
        assertTrue(generatedPassword.chars().anyMatch(character -> "!@#$%^&*()-_=+[]{};:,.?".indexOf(character) >= 0));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.OWNER, userCaptor.getValue().getRole());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
    }
}
