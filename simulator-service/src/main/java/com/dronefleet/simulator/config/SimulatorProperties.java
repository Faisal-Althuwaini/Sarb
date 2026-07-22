package com.dronefleet.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the fleet simulation, bound from {@code sarb.simulator.*} in
 * application.properties. Kept configurable so the demo can "crank up" drone
 * count / tick rate to show the system under load (brief Section 8).
 */
@ConfigurationProperties(prefix = "sarb.simulator")
public class SimulatorProperties {

	/** Number of virtual drones to seed at startup. */
	private int droneCount = 12;

	/** Simulation tick period, in milliseconds. */
	private long tickMs = 400;

	/** Demo area center latitude (Riyadh). */
	private double centerLat = 24.71;

	/** Demo area center longitude (Riyadh). */
	private double centerLon = 46.68;

	/** Radius (km) around the center within which drones are seeded and kept. */
	private double spreadRadiusKm = 5.0;

	public int getDroneCount() {
		return droneCount;
	}

	public void setDroneCount(int droneCount) {
		this.droneCount = droneCount;
	}

	public long getTickMs() {
		return tickMs;
	}

	public void setTickMs(long tickMs) {
		this.tickMs = tickMs;
	}

	public double getCenterLat() {
		return centerLat;
	}

	public void setCenterLat(double centerLat) {
		this.centerLat = centerLat;
	}

	public double getCenterLon() {
		return centerLon;
	}

	public void setCenterLon(double centerLon) {
		this.centerLon = centerLon;
	}

	public double getSpreadRadiusKm() {
		return spreadRadiusKm;
	}

	public void setSpreadRadiusKm(double spreadRadiusKm) {
		this.spreadRadiusKm = spreadRadiusKm;
	}
}
