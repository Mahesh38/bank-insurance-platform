FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs ./libs
COPY services/direct-insurer-adapter-service ./services/direct-insurer-adapter-service
RUN chmod +x gradlew && ./gradlew :services:direct-insurer-adapter-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /workspace/services/direct-insurer-adapter-service/build/libs/direct-insurer-adapter-service.jar app.jar
USER appuser
EXPOSE 8105
ENTRYPOINT ["java", "-jar", "/app.jar"]
