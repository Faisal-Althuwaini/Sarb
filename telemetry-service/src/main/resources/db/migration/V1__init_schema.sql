CREATE TABLE flight_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drone_id VARCHAR(64) NOT NULL,
    ts TIMESTAMPTZ NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    altitude_m DOUBLE PRECISION NOT NULL,
    speed_mps DOUBLE PRECISION NOT NULL,
    heading_deg DOUBLE PRECISION NOT NULL,
    battery_pct DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    mission_id VARCHAR(64)
);

CREATE INDEX idx_flight_logs_drone_ts ON flight_logs (drone_id, ts);
