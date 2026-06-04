package com.LHZ.TripMate.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "TestSecretKeyForJwtUtilTestTestSecretKeyForJwtUtilTest",
            86400000L
        );
    }

    @Test
    void generateToken_thenExtractUsername_returnsSame() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void generateToken_thenExtractRole_returnsSame() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void validToken_isValid() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.isValid(token, "admin")).isTrue();
    }

    @Test
    void tokenWithWrongUsername_isNotValid() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.isValid(token, "other")).isFalse();
    }
}
