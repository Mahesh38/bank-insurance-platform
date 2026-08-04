# Combined image: bank-persistence-service (:8081) + 1sb-integration-service (:8080)
# in a single container, for standalone cloud validation environments where you want
# to hit the 1SB integration Swagger UI end-to-end without provisioning two separate
# containers/services. For production, prefer deploying each service from its own
# Dockerfile (services/bank-persistence-service/Dockerfile,
# services/1sb-integration-service/Dockerfile) as independently scaled containers,
# and point 1sb-integration-service at a real bank-persistence-service + Postgres
# via BANK_PERSISTENCE_BASE_URL / DATASOURCE_*.
#
# Build (context MUST be the repo root):
#   docker build -t bank-insurance-combined .
#
# Run (bank-persistence-service uses in-memory H2 by default — no external DB needed
# for this combined image; override DATASOURCE_* to point at a real Postgres instead):
#   docker run -p 8080:8080 -p 8081:8081 \
#     -e RAW_PAYLOAD_ENCRYPTION_KEY=$(openssl rand -base64 32) \
#     -e ONESB_API_KEY=your_sandbox_key \
#     -e ONESB_API_SECRET=your_sandbox_secret \
#     -e ONESB_DISTRIBUTOR_ID=your_distributor_id \
#     bank-insurance-combined
#
# Swagger UI (after the container is healthy):
#   1SB integration API : http://<host>:8080/swagger-ui.html
#   Persistence API      : http://<host>:8081/swagger-ui.html   (internal-only in real deployments)
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Cache Gradle wrapper + dependency metadata before copying source
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs ./libs
COPY services/bank-persistence-service ./services/bank-persistence-service
COPY services/1sb-integration-service ./services/1sb-integration-service

RUN chmod +x gradlew && \
    ./gradlew :services:bank-persistence-service:bootJar :services:1sb-integration-service:bootJar \
        --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser

WORKDIR /app
COPY --from=build /workspace/services/bank-persistence-service/build/libs/bank-persistence-service.jar bank-persistence-service.jar
COPY --from=build /workspace/services/1sb-integration-service/build/libs/1sb-integration-service.jar 1sb-integration-service.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && chown -R appuser:appuser /app

USER appuser

EXPOSE 8081 8080

# bank-persistence-service: no active Spring profile by default => falls back to the
# in-memory H2 datasource baked into application.yml. Set PERSISTENCE_SPRING_PROFILES_ACTIVE
# (e.g. uat/prod) plus DATASOURCE_URL/USERNAME/PASSWORD to use a real Postgres instead.
ENV PERSISTENCE_SPRING_PROFILES_ACTIVE=""
# Dev-only AES-256 key so the image boots out of the box — override for anything beyond
# throwaway validation (generate your own with: openssl rand -base64 32).
ENV RAW_PAYLOAD_ENCRYPTION_KEY="lo9brrfnf6z7mhenhZfaKMXychPqEjzRtm2zCZKDUos="

# 1sb-integration-service: talks to the persistence service over localhost in this image.
ENV ONESB_SPRING_PROFILES_ACTIVE=uat
ENV INSURANCE_SECRETS_SOURCE=ENV
ENV BANK_PERSISTENCE_BASE_URL=http://localhost:8081
ENV ONESB_BASE_URL=https://demo.api.1silverbullet.tech
# ONESB_API_KEY / ONESB_API_SECRET / ONESB_DISTRIBUTOR_ID are required at runtime — pass
# them with `docker run -e` (or your cloud platform's secret/env config); there is no
# default since they are per-tenant 1SB sandbox credentials.

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8081/actuator/health/liveness \
        && curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
