# 1sb-integration-service
#
# Build context MUST be the repository root (multi-module Gradle build), e.g.:
#   docker build -f services/1sb-integration-service/Dockerfile -t 1sb-integration-service .
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Cache Gradle wrapper + dependency metadata before copying source
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs ./libs
COPY services/1sb-integration-service ./services/1sb-integration-service

RUN chmod +x gradlew && \
    ./gradlew :services:1sb-integration-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /workspace/services/1sb-integration-service/build/libs/1sb-integration-service.jar app.jar
USER appuser

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=uat
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
