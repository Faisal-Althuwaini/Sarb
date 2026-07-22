package com.dronefleet.simulator.simulation;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dronefleet.simulator.config.SimulatorProperties;
import com.dronefleet.simulator.model.Drone;
import com.dronefleet.simulator.model.DroneStatus;
import com.dronefleet.simulator.model.TelemetryFrame;
import com.dronefleet.simulator.registry.DroneRegistry;

/**
 * Seeds the virtual fleet around the demo center (Riyadh) and, on a fixed
 * tick, advances each in-flight drone along its heading with slight jitter,
 * drains battery, keeps drones roughly within the demo area, and broadcasts
 * the whole fleet's telemetry to {@code /topic/telemetry} (brief Section 8).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SimulationEngineImpl implements SimulationEngine {

	/** Meters per degree of latitude (approximately constant everywhere). */
	private static final double METERS_PER_DEGREE_LAT = 111_320.0;

	private static final double LOW_BATTERY_THRESHOLD = 25.0;
	private static final double CRITICAL_BATTERY_THRESHOLD = 8.0;

	private final SimulatorProperties properties;
	private final DroneRegistry registry;
	private final SimpMessagingTemplate messagingTemplate;

	@Override
	@PostConstruct
	public void seedFleet() {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		int count = properties.droneCount();
		for (int i = 0; i < count; i++) {
			String droneId = "drone-%03d".formatted(i + 1);
			double[] point = randomPointWithinRadius(rnd, properties.centerLat(), properties.centerLon(),
					properties.spreadRadiusKm());
			double headingDeg = rnd.nextDouble(0, 360);
			double speedMps = rnd.nextDouble(6.0, 16.0);
			double altitudeM = rnd.nextDouble(40.0, 120.0);
			// Spread battery across the full range so the demo shows every
			// color band (green/amber/red) in the frontend from the start.
			double batteryPct = rnd.nextDouble(12.0, 100.0);
			// A handful of drones start IDLE (grounded) for visual variety.
			DroneStatus status = (i % 5 == 0) ? DroneStatus.IDLE : DroneStatus.IN_FLIGHT;
			if (status == DroneStatus.IN_FLIGHT && batteryPct <= LOW_BATTERY_THRESHOLD) {
				status = DroneStatus.LOW_BATTERY;
			}

			Drone drone = new Drone(droneId, point[0], point[1], altitudeM, speedMps, headingDeg, batteryPct, status,
					null);
			registry.register(drone);
		}
		log.info("Seeded {} virtual drones around ({}, {}), radius {} km", registry.size(),
				properties.centerLat(), properties.centerLon(), properties.spreadRadiusKm());
	}

	@Override
	@Scheduled(fixedRateString = "${sarb.simulator.tick-ms}")
	public void tick() {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		double tickSeconds = properties.tickMs() / 1000.0;

		for (Drone drone : registry.all()) {
			advance(drone, rnd, tickSeconds);
		}

		List<TelemetryFrame> frames = registry.all().stream().map(TelemetryFrame::from).toList();
		messagingTemplate.convertAndSend("/topic/telemetry", frames);
	}

	private void advance(Drone drone, ThreadLocalRandom rnd, double tickSeconds) {
		drone.setLastUpdate(Instant.now());

		if (drone.getStatus() == DroneStatus.LANDED || drone.getStatus() == DroneStatus.IDLE) {
			// Grounded drones don't move; battery stays put.
			return;
		}

		// Jitter the heading a little each tick so movement looks natural,
		// not perfectly straight-line.
		double jitterDeg = rnd.nextDouble(-8.0, 8.0);
		double headingDeg = normalizeDegrees(drone.getHeadingDeg() + jitterDeg);

		double distanceM = drone.getSpeedMps() * tickSeconds;
		double headingRad = Math.toRadians(headingDeg);
		double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(drone.getLat()));

		double newLat = drone.getLat() + (distanceM * Math.cos(headingRad)) / METERS_PER_DEGREE_LAT;
		double newLon = drone.getLon() + (distanceM * Math.sin(headingRad)) / metersPerDegreeLon;

		// Keep drones roughly within the demo area: once beyond the spread
		// radius, steer back toward the center instead of wandering off.
		double distanceFromCenterKm = haversineKm(properties.centerLat(), properties.centerLon(), newLat,
				newLon);
		if (distanceFromCenterKm > properties.spreadRadiusKm()) {
			headingDeg = normalizeDegrees(bearingDegrees(newLat, newLon, properties.centerLat(),
					properties.centerLon()) + rnd.nextDouble(-10.0, 10.0));
			headingRad = Math.toRadians(headingDeg);
			newLat = drone.getLat() + (distanceM * Math.cos(headingRad)) / METERS_PER_DEGREE_LAT;
			newLon = drone.getLon() + (distanceM * Math.sin(headingRad)) / metersPerDegreeLon;
		}

		drone.setHeadingDeg(headingDeg);
		drone.setLat(newLat);
		drone.setLon(newLon);

		// Drain battery a little each tick; rate varies slightly per drone
		// via jitter so drones don't all cross thresholds in lockstep.
		double drainPerTick = (0.015 + rnd.nextDouble(0, 0.02)) * (tickSeconds / 0.4);
		double newBattery = Math.max(0.0, drone.getBatteryPct() - drainPerTick);
		drone.setBatteryPct(newBattery);

		if (newBattery <= 0.0) {
			drone.setStatus(DroneStatus.LANDED);
			drone.setSpeedMps(0.0);
		} else if (newBattery <= CRITICAL_BATTERY_THRESHOLD || newBattery <= LOW_BATTERY_THRESHOLD) {
			drone.setStatus(DroneStatus.LOW_BATTERY);
		}
	}

	private static double normalizeDegrees(double deg) {
		double d = deg % 360.0;
		return d < 0 ? d + 360.0 : d;
	}

	private static double[] randomPointWithinRadius(ThreadLocalRandom rnd, double centerLat, double centerLon,
			double radiusKm) {
		double r = radiusKm * Math.sqrt(rnd.nextDouble());
		double theta = rnd.nextDouble(0, 2 * Math.PI);
		double deltaLat = (r * Math.cos(theta)) / (METERS_PER_DEGREE_LAT / 1000.0);
		double deltaLon = (r * Math.sin(theta)) / ((METERS_PER_DEGREE_LAT / 1000.0) * Math.cos(Math.toRadians(centerLat)));
		return new double[] { centerLat + deltaLat, centerLon + deltaLon };
	}

	private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double r = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return r * c;
	}

	private static double bearingDegrees(double fromLat, double fromLon, double toLat, double toLon) {
		double lat1 = Math.toRadians(fromLat);
		double lat2 = Math.toRadians(toLat);
		double dLon = Math.toRadians(toLon - fromLon);
		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
		return normalizeDegrees(Math.toDegrees(Math.atan2(y, x)));
	}
}
