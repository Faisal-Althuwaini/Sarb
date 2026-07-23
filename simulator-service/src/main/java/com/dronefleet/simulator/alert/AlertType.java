package com.dronefleet.simulator.alert;

/**
 * Alert kinds from the brief (Section 7). LOST_SIGNAL needs a decoupled,
 * timeout-watching consumer to make sense and is deferred to Phase 3 (Kafka).
 */
public enum AlertType {
	GEOFENCE_BREACH,
	ALTITUDE_VIOLATION,
	LOW_BATTERY,
	CRITICAL_BATTERY
}
