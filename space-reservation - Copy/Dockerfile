# ─────────────────────────────────────────────────────────
# Stage 1: Build (Maven)
# ─────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 의존성 캐시 최적화: pom.xml 먼저 복사
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 소스 코드 복사 후 빌드
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────────────────
# Stage 2: Runtime (JRE only - 이미지 크기 최소화)
# ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# 보안: root 사용자 대신 전용 유저 생성
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# 빌드 결과물 복사
COPY --from=builder /app/target/*.jar app.jar

# 로그 디렉토리
RUN mkdir -p logs && chown spring:spring logs

USER spring

# JVM 최적화 옵션
ENV JAVA_OPTS="-Xms256m -Xmx512m \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
