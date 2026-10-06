package com.paymentprocessing.service;

import com.paymentprocessing.dto.RegisterRequest;
import com.paymentprocessing.entity.User;
import com.paymentprocessing.exception.UserAlreadyExistsException;
import com.paymentprocessing.repository.UserRepository;
import com.paymentprocessing.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void registerHashesPasswordAndReturnsToken() {
        var encoder = new BCryptPasswordEncoder();
        var jwtService = new JwtService("test-secret-test-secret-test-secret-test-secret", 60_000);
        var service = new AuthService(userRepository, encoder, authenticationManager, jwtService);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(new RegisterRequest("USER@example.com", "password123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(encoder.matches("password123", userCaptor.getValue().getPassword())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        var service = new AuthService(
                userRepository,
                new BCryptPasswordEncoder(),
                authenticationManager,
                new JwtService("test-secret-test-secret-test-secret-test-secret", 60_000));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("user@example.com", "password123")))
                .isInstanceOf(UserAlreadyExistsException.class);
    }
}
