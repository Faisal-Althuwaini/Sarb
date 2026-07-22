package com.dronefleet.simulator.model;

import java.time.Instant;

/**
 * In-memory drone state, mutated once per tick by {@code SimulationEngine}.
 * Not thread-safe by itself; safe here because all mutation happens on the
 * single {@code @Scheduled} tick thread, while reads (e.g. telemetry
 * serialization) happen right after on the same thread.
 */
public class Drone {

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

	public String getDroneId() {
		return droneId;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getAltitudeM() {
		return altitudeM;
	}

	public void setAltitudeM(double altitudeM) {
		this.altitudeM = altitudeM;
	}

	public double getSpeedMps() {
		return speedMps;
	}

	public void setSpeedMps(double speedMps) {
		this.speedMps = speedMps;
	}

	public double getHeadingDeg() {
		return headingDeg;
	}

	public void setHeadingDeg(double headingDeg) {
		this.headingDeg = headingDeg;
	}

	public double getBatteryPct() {
		return batteryPct;
	}

	public void setBatteryPct(double batteryPct) {
		this.batteryPct = batteryPct;
	}

	public DroneStatus getStatus() {
		return status;
	}

	public void setStatus(DroneStatus status) {
		this.status = status;
	}

	public String getMissionId() {
		return missionId;
	}

	public void setMissionId(String missionId) {
		this.missionId = missionId;
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
