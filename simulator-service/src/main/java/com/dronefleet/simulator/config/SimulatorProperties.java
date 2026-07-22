package com.dronefleet.simulator.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for the fleet simulation, bound from {@code sarb.simulator.*} in
 * application.properties. Kept configurable so the demo can "crank up" drone
 * count / tick rate to show the system under load (brief Section 8).
 */
@Validated
@ConfigurationProperties(prefix = "sarb.simulator")
public record SimulatorProperties(
		/** Number of virtual drones to seed at startup. */
		@Min(1) int droneCount,

		/** Simulation tick period, in milliseconds. */
		@Min(50) long tickMs,

		/** Demo area center latitude (Riyadh). */
		double centerLat,

		/** Demo area center longitude (Riyadh). */
		double centerLon,

		/** Radius (km) around the center within which drones are seeded and kept. */
		@Positive double spreadRadiusKm) {
}
