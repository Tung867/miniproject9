package com.spaceres.config;

import com.spaceres.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 전체 설정
 *
 * - Stateless (JWT 사용으로 세션 없음)
 * - 공개 경로 / 인증 필요 경로 / 관리자 전용 경로 분리
 * - 401 / 403 커스텀 핸들러 적용
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // @PreAuthorize, @Secured 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // ── 공개 접근 허용 경로 ───────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    // ── 관리자 전용 경로 ──────────────────────────────────
    private static final String[] ADMIN_URLS = {
            "/api/admin/**",
            "/actuator/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless 세션 정책
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청별 인증/인가 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(ADMIN_URLS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/spaces/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/**").authenticated()
                        .anyRequest().authenticated()
                )

                // 커스텀 예외 핸들러
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )

                // AuthenticationProvider 설정
                .authenticationProvider(authenticationProvider())

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── 401 Unauthorized 핸들러 ───────────────────────────
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"success":false,"message":"인증이 필요합니다. 로그인 후 이용해주세요.","code":"UNAUTHORIZED"}
                    """);
        };
    }

    // ── 403 Forbidden 핸들러 ──────────────────────────────
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"success":false,"message":"접근 권한이 없습니다.","code":"FORBIDDEN"}
                    """);
        };
    }
}
