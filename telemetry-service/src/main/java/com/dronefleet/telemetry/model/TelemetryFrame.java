package com.dronefleet.telemetry.model;

import java.time.Instant;

/**
 * Mirrors simulator-service's {@code TelemetryFrame} field-for-field - the
 * {@code drone.telemetry} Kafka payload shape (brief Section 7). Each
 * consumer service owns its own copy of this contract rather than sharing a
 * library module, consistent with keeping services independently deployable.
 */
public record TelemetryFrame(
		String droneId,
		Instant timestamp,
		Position position,
		double altitudeM,
		double speedMps,
		double headingDeg,
		double batteryPct,
		String missionId,
		DroneStatus status) {
}
