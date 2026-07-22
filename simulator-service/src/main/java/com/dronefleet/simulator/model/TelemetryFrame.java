package com.dronefleet.simulator.model;

import java.time.Instant;

/**
 * Wire shape broadcast on {@code /topic/telemetry}, matching the brief's
 * illustrative telemetry message (Section 7) field-for-field. Jackson
 * serializes {@link Instant} as an ISO-8601 string by default in Spring Boot.
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

	public static TelemetryFrame from(Drone drone) {
		return new TelemetryFrame(
				drone.getDroneId(),
				drone.getLastUpdate(),
				new Position(drone.getLat(), drone.getLon()),
				drone.getAltitudeM(),
				drone.getSpeedMps(),
				drone.getHeadingDeg(),
				drone.getBatteryPct(),
				drone.getMissionId(),
				drone.getStatus());
	}
}
