package com.dronefleet.alert.model;

/**
 * Alert kinds from the brief (Section 7). LOST_SIGNAL needs a decoupled,
 * timeout-watching consumer (no telemetry arriving at all, rather than a
 * telemetry-derived condition) and stays deferred - a natural Phase 3+
 * follow-up now that alert-service is an independent Kafka consumer.
 */
public enum AlertType {
	GEOFENCE_BREACH,
	ALTITUDE_VIOLATION,
	LOW_BATTERY,
	CRITICAL_BATTERY
}
