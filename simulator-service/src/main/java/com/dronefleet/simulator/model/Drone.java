package com.dronefleet.simulator.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * In-memory drone state, mutated once per tick by {@code SimulationEngine}.
 * Not thread-safe by itself; safe here because all mutation happens on the
 * single {@code @Scheduled} tick thread, while reads (e.g. telemetry
 * serialization) happen right after on the same thread.
 */
@Getter
@Setter
public class Drone {

	// Immutable identity — Lombok skips generating a setter for final fields.
	private final String droneId;

	private double lat;
	private double lon;
	private double altitudeM;
	private double speedMps;
	private double headingDeg;
	private double batteryPct;
	private DroneStatus status;
	private String missionId;
	private Instant lastUpdate;

	public Drone(String droneId, double lat, double lon, double altitudeM, double speedMps,
			double headingDeg, double batteryPct, DroneStatus status, String missionId) {
		this.droneId = droneId;
		this.lat = lat;
		this.lon = lon;
		this.altitudeM = altitudeM;
		this.speedMps = speedMps;
		this.headingDeg = headingDeg;
		this.batteryPct = batteryPct;
		this.status = status;
		this.missionId = missionId;
		this.lastUpdate = Instant.now();
	}
}
