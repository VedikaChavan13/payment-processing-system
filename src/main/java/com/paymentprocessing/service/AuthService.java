package com.paymentprocessing.service;

import com.paymentprocessing.dto.AuthResponse;
import com.paymentprocessing.dto.LoginRequest;
import com.paymentprocessing.dto.RegisterRequest;
import com.paymentprocessing.entity.User;
import com.paymentprocessing.exception.UserAlreadyExistsException;
import com.paymentprocessing.repository.UserRepository;
import com.paymentprocessing.security.JwtService;
import com.paymentprocessing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException();
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password())));
        return AuthResponse.bearer(jwtService.generateToken(new UserPrincipal(user.getId(), user.getEmail(), user.getPassword())));
    }

    public AuthResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        return AuthResponse.bearer(jwtService.generateToken((UserPrincipal) auth.getPrincipal()));
    }
}
