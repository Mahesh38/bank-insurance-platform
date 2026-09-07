FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs ./libs
COPY services/customer-service ./services/customer-service
RUN chmod +x gradlew && ./gradlew :services:customer-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /workspace/services/customer-service/build/libs/customer-service.jar app.jar
USER appuser
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app.jar"]
