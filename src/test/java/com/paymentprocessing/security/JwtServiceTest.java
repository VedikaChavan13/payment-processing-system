package com.paymentprocessing.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void generatesAndValidatesJwt() {
        JwtService jwtService = new JwtService("test-secret-test-secret-test-secret-test-secret", 60_000);
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@example.com", "hash");

        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(principal.id());
        assertThat(jwtService.isValid(token, principal)).isTrue();
    }
}
