package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.riftlog.dto.AuthResponse;
import com.riftlog.dto.LoginRequest;
import com.riftlog.dto.RegisterRequest;
import com.riftlog.entity.User;
import com.riftlog.exception.EmailAlreadyUsedException;
import com.riftlog.exception.InvalidCredentialsException;
import com.riftlog.repository.UserRepository;
import com.riftlog.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("paul@example.com");
        existingUser.setPasswordHash("hashed-password");
        existingUser.setDisplayName("Paul");
    }

    @Test
    void register_createsUserWithHashedPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New Player");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals(2L, response.userId());
        assertEquals("new@example.com", response.email());
        assertEquals("New Player", response.displayName());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("paul@example.com", "password123", "Paul");
        when(userRepository.existsByEmailIgnoreCase("paul@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyUsedException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenWhenCredentialsMatch() {
        when(userRepository.findByEmailIgnoreCase("paul@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("paul@example.com", "correct-password"));

        assertEquals("jwt-token", response.token());
        assertEquals(1L, response.userId());
    }

    @Test
    void login_rejectsWrongPassword() {
        when(userRepository.findByEmailIgnoreCase("paul@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("paul@example.com", "wrong-password")));
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ghost@example.com", "whatever")));
    }
}
