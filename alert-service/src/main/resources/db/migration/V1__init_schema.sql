CREATE TABLE alerts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drone_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    message TEXT NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_alerts_active ON alerts (drone_id, type) WHERE resolved_at IS NULL;
CREATE INDEX idx_alerts_triggered_at ON alerts (triggered_at);
