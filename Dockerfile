# ─── 빌드 단계 ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Gradle Wrapper만 먼저 복사해 의존성 캐시 활용
COPY gradle/ gradle/
COPY gradlew gradlew
COPY gradle.properties* ./
COPY settings.gradle.kts build.gradle.kts ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# 소스 복사 후 JAR 빌드
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ─── 런타임 단계 ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/build/libs/gymcheck-*.jar app.jar

EXPOSE 8080

# SPRING_PROFILES_ACTIVE 환경변수로 프로파일 지정 (Railway Variables에서 설정)
ENTRYPOINT ["java", "-jar", "app.jar"]
