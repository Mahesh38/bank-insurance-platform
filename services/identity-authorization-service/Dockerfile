FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs ./libs
COPY services/identity-authorization-service ./services/identity-authorization-service
RUN chmod +x gradlew && ./gradlew :services:identity-authorization-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /workspace/services/identity-authorization-service/build/libs/identity-authorization-service.jar app.jar
USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
