package com.spaceres.service;

import com.spaceres.dto.request.LoginRequest;
import com.spaceres.dto.request.SignUpRequest;
import com.spaceres.dto.response.TokenResponse;
import com.spaceres.entity.User;
import com.spaceres.exception.BusinessException;
import com.spaceres.exception.ErrorCode;
import com.spaceres.repository.UserRepository;
import com.spaceres.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // ── 회원가입 ──────────────────────────────────────────
    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .organization(request.getOrganization())
                .role(User.Role.USER)
                .build();

        userRepository.save(user);
        log.info("신규 회원 등록: {}", request.getEmail());
    }

    // ── 로그인 ────────────────────────────────────────────
    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token DB 저장
        userRepository.updateRefreshToken(user.getEmail(), refreshToken);

        log.info("로그인 성공: {}", user.getEmail());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    // ── 토큰 재발급 ───────────────────────────────────────
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // Refresh Token 유효성 검사
        if (!jwtTokenProvider.isTokenValid(refreshToken, user)) {
            userRepository.updateRefreshToken(user.getEmail(), null);
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token Rotation
        userRepository.updateRefreshToken(user.getEmail(), newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    // ── 로그아웃 (Refresh Token 삭제) ─────────────────────
    @Transactional
    public void logout(String email) {
        userRepository.updateRefreshToken(email, null);
        log.info("로그아웃: {}", email);
    }
}
