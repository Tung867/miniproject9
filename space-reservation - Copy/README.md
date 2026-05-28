# 🏢 Space Reservation System — Part C: BE Infra

> **담당**: C (진딘동)
> **역할**: JWT 인증 · Google Drive 백업 · Docker · CI/CD · 서버 모니터링

---

## 📁 프로젝트 구조

```
src/main/java/com/spaceres/
├── SpaceReservationApplication.java   # 메인 진입점
├── config/
│   ├── SecurityConfig.java            # Spring Security 설정
│   ├── SwaggerConfig.java             # Swagger UI 설정
│   └── GoogleDriveConfig.java         # Google Drive API 설정
├── security/
│   ├── jwt/
│   │   └── JwtTokenProvider.java      # JWT 생성/검증
│   └── filter/
│       └── JwtAuthenticationFilter.java  # JWT 요청 필터
├── entity/
│   └── User.java                      # 사용자 엔티티 + UserDetails
├── repository/
│   └── UserRepository.java
├── service/
│   ├── AuthService.java               # 로그인/회원가입/토큰 재발급
│   ├── CustomUserDetailsService.java
│   └── GoogleDriveBackupService.java  # 드라이브 백업 + 스케줄러
├── controller/
│   ├── AuthController.java            # /api/auth/**
│   └── BackupController.java          # /api/admin/backup/**
├── dto/
│   ├── request/ (SignUpRequest, LoginRequest)
│   └── response/ (ApiResponse, TokenResponse)
└── exception/
    ├── BusinessException.java
    ├── ErrorCode.java
    └── GlobalExceptionHandler.java
```

---

## 🚀 로컬 실행 방법

### 1. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일 열어서 값 수정
```

### 2. Google Service Account 설정

```bash
mkdir -p config
# Google Cloud Console에서 받은 JSON을 아래 경로에 저장
cp ~/Downloads/your-credentials.json config/google-credentials.json
```

### 3. Docker로 전체 실행

```bash
docker-compose up -d
```

### 4. 개별 Spring Boot 실행 (개발 중)

```bash
# MySQL 먼저 실행
docker-compose up -d db

# Spring Boot 실행
./mvnw spring-boot:run
```

---

## 🔐 API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 → 토큰 발급 | ❌ |
| POST | `/api/auth/reissue` | Access Token 재발급 | Refresh Token |
| POST | `/api/auth/logout` | 로그아웃 | ✅ JWT |
| POST | `/api/admin/backup/run` | 수동 백업 | ✅ ADMIN |
| GET  | `/api/admin/backup/files` | 백업 목록 | ✅ ADMIN |
| DELETE | `/api/admin/backup/cleanup` | 오래된 파일 정리 | ✅ ADMIN |

**Swagger UI**: http://localhost:8080/swagger-ui.html

---

## 🔑 JWT 인증 흐름

```
1. POST /api/auth/login
   → 서버: Access Token (30분) + Refresh Token (7일) 발급
   → Refresh Token은 DB에 저장

2. API 요청 시: Authorization: Bearer {accessToken}

3. Access Token 만료 시:
   POST /api/auth/reissue
   Header: Refresh-Token: {refreshToken}
   → 새 Access Token + Refresh Token 발급 (Rotation)

4. 로그아웃: Refresh Token DB에서 삭제
```

---

## ☁️ Google Drive 백업

- **자동 백업**: 매일 새벽 2시 (KST) 자동 실행
- **수동 백업**: `POST /api/admin/backup/run` (관리자 토큰 필요)
- **보관 정책**: 최대 30개 파일 유지, 초과 시 오래된 파일 자동 삭제

### Google Service Account 발급 방법

```
1. Google Cloud Console → IAM → 서비스 계정 생성
2. Drive API 권한 부여
3. JSON 키 다운로드 → config/google-credentials.json 저장
4. Google Drive에서 백업 폴더 생성
5. 서비스 계정 이메일에 폴더 편집 권한 부여
6. 폴더 ID를 .env의 GOOGLE_DRIVE_FOLDER_ID에 입력
```

---

## 📊 모니터링

| 서비스 | URL | 계정 |
|--------|-----|------|
| Grafana | http://localhost:3000 | admin / .env 참조 |
| Prometheus | http://localhost:9090 | - |
| Health Check | http://localhost:8080/actuator/health | - |

---

## 🔄 CI/CD (GitHub Actions)

| 브랜치 | 동작 |
|--------|------|
| `feature/*` → PR | 테스트만 실행 |
| `develop` push | 테스트 + Docker 이미지 빌드/Push |
| `main` push | 테스트 + 빌드 + **서버 자동 배포** |

### GitHub Secrets 설정 (main 배포 전 필요)

```
DEPLOY_HOST      - 서버 IP
DEPLOY_USER      - SSH 사용자명
DEPLOY_SSH_KEY   - SSH 개인키
```

---

## 🧪 테스트 실행

```bash
./mvnw test
```
