package com.spaceres.security;

import com.spaceres.entity.User;
import com.spaceres.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWT Token Provider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-for-unit-testing-minimum-256-bits-long-ok";
        long accessExp  = 1800000L;  // 30분
        long refreshExp = 604800000L; // 7일

        jwtTokenProvider = new JwtTokenProvider(secret, accessExp, refreshExp);

        testUser = User.builder()
                .email("test@example.com")
                .password("encoded_password")
                .name("테스트유저")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("Access Token 생성 및 username 추출 성공")
    void generateAccessToken_andExtractUsername() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Access Token 유효성 검사 성공")
    void validateAccessToken_success() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertThat(jwtTokenProvider.isTokenValid(token, testUser)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("Refresh Token은 Access Token이 아님")
    void refreshToken_isNotAccessToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("다른 사용자의 토큰은 유효하지 않음")
    void validateToken_withDifferentUser_fails() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        UserDetails otherUser = User.builder()
                .email("other@example.com")
                .password("encoded")
                .role(User.Role.USER)
                .build();

        assertThat(jwtTokenProvider.isTokenValid(token, otherUser)).isFalse();
    }
}
