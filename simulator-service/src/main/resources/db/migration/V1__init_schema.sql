-- Phase 2: flight log + alert persistence (brief Section 12).

CREATE TABLE flight_logs (
    id          BIGSERIAL PRIMARY KEY,
    drone_id    VARCHAR(64) NOT NULL,
    ts          TIMESTAMPTZ NOT NULL,
    lat         DOUBLE PRECISION NOT NULL,
    lon         DOUBLE PRECISION NOT NULL,
    altitude_m  DOUBLE PRECISION NOT NULL,
    speed_mps   DOUBLE PRECISION NOT NULL,
    heading_deg DOUBLE PRECISION NOT NULL,
    battery_pct DOUBLE PRECISION NOT NULL,
    status      VARCHAR(32) NOT NULL,
    mission_id  VARCHAR(64)
);

CREATE INDEX idx_flight_logs_drone_ts ON flight_logs (drone_id, ts);

CREATE TABLE alerts (
    id           BIGSERIAL PRIMARY KEY,
    drone_id     VARCHAR(64) NOT NULL,
    type         VARCHAR(32) NOT NULL,
    severity     VARCHAR(16) NOT NULL,
    message      TEXT NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    resolved_at  TIMESTAMPTZ
);

-- Partial index: the hot lookup is "is this drone/type already alerting?",
-- which only ever matters while resolved_at is still null.
CREATE INDEX idx_alerts_active ON alerts (drone_id, type) WHERE resolved_at IS NULL;
CREATE INDEX idx_alerts_triggered_at ON alerts (triggered_at);
