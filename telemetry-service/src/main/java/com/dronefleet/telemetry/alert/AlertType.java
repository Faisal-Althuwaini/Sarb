package com.dronefleet.telemetry.alert;

/** Mirrors alert-service's enum - only used here to relay {@code drone.alerts} onto STOMP. */
public enum AlertType {
	GEOFENCE_BREACH,
	ALTITUDE_VIOLATION,
	LOW_BATTERY,
	CRITICAL_BATTERY
}
