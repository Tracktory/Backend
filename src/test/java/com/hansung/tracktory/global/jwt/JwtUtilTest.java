package com.hansung.tracktory.global.jwt;

import com.hansung.tracktory.domain.user.service.UserPrincipal;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0xMjM0NTY=";
    private static final long EXPIRATION = 86400000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_이메일과userId_포함() {
        UserPrincipal principal = UserPrincipal.of(1L, "a@b.com");
        String token = jwtUtil.generateToken(principal);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("a@b.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    void isTokenValid_유효한토큰_true() {
        String token = jwtUtil.generateToken(UserPrincipal.of(1L, "a@b.com"));

        assertThat(jwtUtil.isTokenValid(token, "a@b.com")).isTrue();
    }

    @Test
    void isTokenValid_이메일불일치_false() {
        String token = jwtUtil.generateToken(UserPrincipal.of(1L, "a@b.com"));

        assertThat(jwtUtil.isTokenValid(token, "other@b.com")).isFalse();
    }

    @Test
    void 만료토큰_파싱시_예외() {
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000L);
        String token = expiredJwtUtil.generateToken(UserPrincipal.of(1L, "a@b.com"));

        assertThatThrownBy(() -> jwtUtil.isTokenValid(token, "a@b.com"))
                .isInstanceOf(JwtException.class);
    }
}
