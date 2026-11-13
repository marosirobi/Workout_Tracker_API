package com.marosirobi.workouttrackerapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // test-only keys
    private static final String SECRET =
            "kx6vbvda8/79jOiUEvwFmXcwrnpaWQilGu2Z2uFRuPUMPOKyfETBMPmbZs/rgGyOZjvjP9n3TJwlM5FibgkO1g==";
    private static final String OTHER_SECRET =
            "FqD5du9F/UNTohpMEuCyirZ67bTD4bsqqgdcJ2ZKNlbFRV/OWY7aceN8o46xBhA6GOtHw60pv+gic3vxob8/LQ==";
    private static final long FIFTEEN_MINUTES_MS = 900_000;

    private final JwtService jwtService = new JwtService(SECRET, FIFTEEN_MINUTES_MS);

    @Test
    void issuedToken_isValid_andCarriesClaims() {
        String token = jwtService.generateToken("user@gmail.com", 42L);

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@gmail.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void tokenSignedWithOtherKey_isInvalid() {
        String foreign = new JwtService(OTHER_SECRET, FIFTEEN_MINUTES_MS).generateToken("user@gmail.com", 42L);

        assertThat(jwtService.validateToken(foreign)).isFalse();
    }

    @Test
    void expiredToken_isInvalid() {
        String expired = new JwtService(SECRET, -1_000).generateToken("user@gmail.com", 42L);

        assertThat(jwtService.validateToken(expired)).isFalse();
    }

    @Test
    void tamperedToken_isInvalid() {
        String token = jwtService.generateToken("user@gmail.com", 42L);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"not.a.jwt", "abc", "a.b.c.d"})
    void malformedToken_isInvalid(String token) {
        assertThat(jwtService.validateToken(token)).isFalse();
    }
}
