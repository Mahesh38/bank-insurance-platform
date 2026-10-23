#!/usr/bin/env bash
# Validate SQL against a real PostgreSQL (16+): the R0 design DDL pack and/or every service's
# Flyway migrations. Each part runs in its own throw-away database that this script creates and
# drops; it never touches any other database on the server.
#
#   pack      docs/platform/data-architecture/schemas/00..15, 90, 91 in README order (test-only
#             NOLOGIN roles from tests/00-test-roles.sql before 91-grants.sql), then the invariant
#             assertions in tests/10-invariants.sql.
#   services  for each services/*/src/main/resources/db/migration: the V*.sql files of
#             db/migration plus db/vendor/postgresql when the service's spring.flyway.locations
#             includes db/vendor/{vendor}, merged in Flyway version order, each file applied in one
#             transaction (Flyway's PostgreSQL default). No Flyway CLI or Docker needed.
#
# Usage: scripts/db/validate-postgres.sh [--host H] [--port P] [--user U] [--keep] [pack|services|all]
#   defaults: host /tmp (Unix socket dir), port 55432, user postgres, target all.
#   Env PGHOST/PGPORT/PGUSER/PGPASSWORD are honoured when the flags are not given.
#   --keep  leave the databases in place for inspection (names are printed).
# Exit: 0 all green, 1 at least one failure, 2 usage / server unreachable.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCHEMAS="$ROOT/docs/platform/data-architecture/schemas"
HOST="${PGHOST:-/tmp}"
PORT="${PGPORT:-55432}"
USER_="${PGUSER:-postgres}"
KEEP=0
TARGET=all

while [ $# -gt 0 ]; do
    case "$1" in
        --host) HOST="$2"; shift 2 ;;
        --port) PORT="$2"; shift 2 ;;
        --user) USER_="$2"; shift 2 ;;
        --keep) KEEP=1; shift ;;
        pack|services|all) TARGET="$1"; shift ;;
        -h|--help) sed -n '2,22p' "$0"; exit 0 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

CONN=(-h "$HOST" -p "$PORT" -U "$USER_")
PSQL=(psql "${CONN[@]}" -X -q -v ON_ERROR_STOP=1)
SUFFIX="$(date +%s)_$$"
CREATED=()
LOG_DIR="$(mktemp -d)"

cleanup() {
    if [ "$KEEP" -eq 0 ]; then
        for db in "${CREATED[@]+"${CREATED[@]}"}"; do
            dropdb "${CONN[@]}" --if-exists "$db" >/dev/null 2>&1
        done
    elif [ "${#CREATED[@]}" -gt 0 ]; then
        echo "kept databases: ${CREATED[*]}"
    fi
    rm -rf "$LOG_DIR"
}
trap cleanup EXIT

if ! pg_isready "${CONN[@]}" >/dev/null 2>&1; then
    echo "PostgreSQL not reachable at host=$HOST port=$PORT" >&2
    exit 2
fi

new_db() {
    local db="$1"
    createdb "${CONN[@]}" "$db" || { echo "createdb $db failed" >&2; exit 2; }
    CREATED+=("$db")
}

# Applies one file; on failure prints the psql error (file:line: ERROR ...) and returns 1.
apply() {
    local db="$1" file="$2" single="${3:-}" out
    if ! out="$("${PSQL[@]}" -d "$db" $single -f "$file" 2>&1)"; then
        printf '    FAIL %s\n' "${file#"$ROOT"/}"
        printf '%s\n' "$out" | grep -v '^NOTICE' | sed 's/^/      /' | head -20
        return 1
    fi
    return 0
}

FAILURES=0

run_pack() {
    local db="vp_pack_$SUFFIX" f ok=1
    echo "== design DDL pack -> $db"
    new_db "$db"
    for f in "$SCHEMAS"/[0-9][0-9]-*.sql; do
        [ "$(basename "$f")" = "91-grants.sql" ] && { apply "$db" "$SCHEMAS/tests/00-test-roles.sql" || ok=0; }
        apply "$db" "$f" || { ok=0; break; }
        echo "    ok   $(basename "$f")"
    done
    if [ "$ok" -eq 1 ]; then
        local out
        if out="$("${PSQL[@]}" -d "$db" -f "$SCHEMAS/tests/10-invariants.sql" 2>&1)"; then
            printf '%s\n' "$out" | grep 'ALL INVARIANT' | sed 's/^ */    /'
        else
            printf '%s\n' "$out" | grep -E 'ERROR|FAIL' | sed 's/^/    /' | head -10
            ok=0
        fi
    fi
    [ "$ok" -eq 1 ] && echo "   PASS design pack" || { echo "   FAIL design pack"; FAILURES=$((FAILURES + 1)); }
}

# Prints "<version>\t<path>" for a migration dir, keeping only versioned V<ver>__*.sql files.
list_versioned() {
    local dir="$1" f v
    [ -d "$dir" ] || return 0
    for f in "$dir"/V*__*.sql; do
        [ -e "$f" ] || continue
        v="$(basename "$f")"; v="${v#V}"; v="${v%%__*}"; v="${v//_/.}"
        printf '%s\t%s\n' "$v" "$f"
    done
}

run_services() {
    local svc_dir svc res cfg db files f ok
    echo "== service Flyway migrations (PostgreSQL vendor)"
    for svc_dir in "$ROOT"/services/*/; do
        svc="$(basename "$svc_dir")"
        res="$svc_dir/src/main/resources"
        [ -d "$res/db/migration" ] || continue
        cfg="$(cat "$res"/application*.y*ml 2>/dev/null)"
        db="vp_svc_${svc//-/_}_$SUFFIX"
        new_db "$db"
        files="$(list_versioned "$res/db/migration")"
        if printf '%s' "$cfg" | grep -q 'db/vendor/{vendor}'; then
            files="$files"$'\n'"$(list_versioned "$res/db/vendor/postgresql")"
        fi
        ok=1
        while IFS=$'\t' read -r _ f; do
            [ -n "$f" ] || continue
            apply "$db" "$f" -1 || { ok=0; break; }
        done < <(printf '%s\n' "$files" | sed '/^$/d' | sort -t$'\t' -k1,1V)
        if [ "$ok" -eq 1 ]; then
            echo "   PASS $svc ($(printf '%s\n' "$files" | sed '/^$/d' | wc -l | tr -d ' ') migrations)"
        else
            echo "   FAIL $svc"
            FAILURES=$((FAILURES + 1))
        fi
    done
}

case "$TARGET" in
    pack) run_pack ;;
    services) run_services ;;
    all) run_pack; run_services ;;
esac

if [ "$FAILURES" -gt 0 ]; then
    echo "RESULT: $FAILURES failing part(s)"
    exit 1
fi
echo "RESULT: all green"
exit 0
