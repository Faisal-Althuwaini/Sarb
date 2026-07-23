CREATE TABLE missions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drone_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE TABLE mission_waypoints (
    mission_id BIGINT NOT NULL REFERENCES missions (id) ON DELETE CASCADE,
    seq INTEGER NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    altitude_m DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (mission_id, seq)
);

CREATE INDEX idx_missions_drone_active ON missions (drone_id) WHERE status IN ('ASSIGNED', 'IN_PROGRESS');
