package com.example.cns.auth;

import com.example.cns.jwt.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {

    private JWTUtil jwtUtil;

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-signing";

    @BeforeEach
    void setUp() {
        jwtUtil = new JWTUtil(SECRET);
    }

    @Test
    void createAndParseToken_success() {
        // given
        String token = jwtUtil.createJwt("user1", "ROLE_USER", 60_000L);

        // when
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);
        boolean expired = jwtUtil.isExpired(token);

        // then
        assertThat(username).isEqualTo("user1");
        assertThat(role).isEqualTo("ROLE_USER");
        assertThat(expired).isFalse();
    }

    @Test
    void getUsername_withBearerPrefix_parsesCorrectly() {
        // given
        String token = jwtUtil.createJwt("admin1", "ROLE_ADMIN", 60_000L);
        String bearerToken = "Bearer " + token;

        // when & then
        assertThat(jwtUtil.getUsername(bearerToken)).isEqualTo("admin1");
        assertThat(jwtUtil.getRole(bearerToken)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void isExpired_expiredToken_throwsException() {
        // given: 이미 만료된 토큰 (-1ms)
        String expiredToken = jwtUtil.createJwt("user1", "ROLE_USER", -1L);

        // when & then: isExpired 호출 시 ExpiredJwtException 발생
        assertThatThrownBy(() -> jwtUtil.isExpired(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
