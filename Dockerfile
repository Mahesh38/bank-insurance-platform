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
# for this combined image; override DATASOURCE_* to point at a real Postgres instead).
# Only 8080 needs to be published — 8081 is internal-only (see EXPOSE note below):
#   docker run -p 8080:8080 \
#     -e RAW_PAYLOAD_ENCRYPTION_KEY=$(openssl rand -base64 32) \
#     -e ONESB_API_KEY=your_sandbox_key \
#     -e ONESB_API_SECRET=your_sandbox_secret \
#     -e ONESB_DISTRIBUTOR_ID=your_distributor_id \
#     bank-insurance-combined
#
# Swagger UI (after the container is healthy):
#   http://<host>:8080/swagger-ui.html
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

EXPOSE 8080
# bank-persistence-service (:8081) is intentionally NOT exposed here: in this combined
# image it is only ever called over localhost by the 1sb-integration-service in the same
# container, never from outside it. This also keeps single-port PaaS platforms (e.g.
# Render) unambiguous about which port to route public traffic to.

# bank-persistence-service: no active Spring profile by default => falls back to the
# in-memory H2 datasource baked into application.yml. Set PERSISTENCE_SPRING_PROFILES_ACTIVE
# (e.g. uat/prod) plus DATASOURCE_URL/USERNAME/PASSWORD to use a real Postgres instead.
ENV PERSISTENCE_SPRING_PROFILES_ACTIVE=""
# RAW_PAYLOAD_ENCRYPTION_KEY has NO default and MUST be supplied at run time.
#
# This previously carried a baked-in base64 AES-256 key so the image would boot
# unattended. That key protects the raw_payload store, which holds PII and is
# retained for 7 years — so a default key committed to the repository means any
# deployment that forgets to override it encrypts regulated data with a key that
# is public. It also contradicted this platform's own rule: "No secrets in
# application.yml, Dockerfile, or source code"
# (docs/1sb-insurance-integration/architecture/1sb-integration-service-architecture.md
# section 8.4). Found by gitleaks under S08-E04-S01; removed rather than allowlisted.
#
# Empty is deliberate and fails CLOSED: RawPayloadEncryptionService refuses to
# start without a valid 32-byte key and says so by name. Generate one with:
#     openssl rand -base64 32
ENV RAW_PAYLOAD_ENCRYPTION_KEY=""

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
