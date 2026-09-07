-- Skeleton schema for {{SERVICE_NAME}} (bounded context #{{CONTEXT_ID}}).
-- Replace with the authoritative physical model from docs/platform/data-architecture/schemas/.

CREATE TABLE IF NOT EXISTS service_metadata (
    id          VARCHAR(64)  PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
