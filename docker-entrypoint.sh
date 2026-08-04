#!/bin/bash
# Starts both services packaged in the combined image:
#   - bank-persistence-service on :8081 (started first; 1SB depends on it)
#   - 1sb-integration-service   on :8080
#
# Waits for the persistence service's liveness probe before starting the 1SB
# service, forwards SIGTERM/SIGINT to both, and exits non-zero if either
# process dies so the container/orchestrator can restart it.
set -euo pipefail

PERSISTENCE_JAR=/app/bank-persistence-service.jar
INTEGRATION_JAR=/app/1sb-integration-service.jar

# Two JVMs share whatever RAM the host/plan gives the container. Cap heaps explicitly
# so both processes fit predictably instead of one OOM-killing the other on smaller
# cloud instance sizes; override via env if you deploy on a bigger plan.
PERSISTENCE_JAVA_OPTS="${PERSISTENCE_JAVA_OPTS:--Xmx384m}"
ONESB_JAVA_OPTS="${ONESB_JAVA_OPTS:--Xmx384m}"

persistence_opts=($PERSISTENCE_JAVA_OPTS)
if [ -n "${PERSISTENCE_SPRING_PROFILES_ACTIVE:-}" ]; then
  persistence_opts+=("-Dspring.profiles.active=${PERSISTENCE_SPRING_PROFILES_ACTIVE}")
fi

integration_opts=($ONESB_JAVA_OPTS)
if [ -n "${ONESB_SPRING_PROFILES_ACTIVE:-}" ]; then
  integration_opts+=("-Dspring.profiles.active=${ONESB_SPRING_PROFILES_ACTIVE}")
fi

persistence_pid=""
integration_pid=""

shutdown() {
  trap - TERM INT
  echo "[entrypoint] shutting down..."
  [ -n "$integration_pid" ] && kill "$integration_pid" 2>/dev/null || true
  [ -n "$persistence_pid" ] && kill "$persistence_pid" 2>/dev/null || true
  [ -n "$integration_pid" ] && wait "$integration_pid" 2>/dev/null || true
  [ -n "$persistence_pid" ] && wait "$persistence_pid" 2>/dev/null || true
}
trap shutdown TERM INT

echo "[entrypoint] starting bank-persistence-service on :8081..."
java "${persistence_opts[@]}" -jar "$PERSISTENCE_JAR" &
persistence_pid=$!

echo "[entrypoint] waiting for bank-persistence-service liveness..."
attempt=0
until curl -fsS http://localhost:8081/actuator/health/liveness >/dev/null 2>&1; do
  if ! kill -0 "$persistence_pid" 2>/dev/null; then
    echo "[entrypoint] bank-persistence-service exited during startup" >&2
    exit 1
  fi
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo "[entrypoint] bank-persistence-service did not become live within 120s" >&2
    shutdown
    exit 1
  fi
  sleep 2
done
echo "[entrypoint] bank-persistence-service is live"

echo "[entrypoint] starting 1sb-integration-service on :8080..."
java "${integration_opts[@]}" -jar "$INTEGRATION_JAR" &
integration_pid=$!

set +e
wait -n "$persistence_pid" "$integration_pid"
exit_code=$?
set -e
echo "[entrypoint] a service process exited (code $exit_code) - stopping the other"
shutdown
exit "$exit_code"
