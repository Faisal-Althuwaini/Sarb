package com.dronefleet.simulator.simulation;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dronefleet.simulator.config.SimulatorProperties;
import com.dronefleet.simulator.geo.GeoMath;
import com.dronefleet.simulator.mission.MissionEventDto;
import com.dronefleet.simulator.mission.MissionEventType;
import com.dronefleet.simulator.model.Drone;
import com.dronefleet.simulator.model.DroneStatus;
import com.dronefleet.simulator.model.Position;
import com.dronefleet.simulator.model.TelemetryFrame;
import com.dronefleet.simulator.registry.DroneRegistry;

/**
 * Seeds the virtual fleet around the demo center (Riyadh) and, on a fixed
 * tick, advances each in-flight drone along its heading with slight jitter,
 * drains battery, keeps drones roughly within the demo area, and produces
 * one {@code drone.telemetry} Kafka message per drone, keyed by droneId so
 * per-drone ordering is preserved across partitions (brief Section 8; Phase 3
 * fan-out - independent consumers pick this up for WebSocket push, flight
 * logging, and alert rules).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SimulationEngineImpl implements SimulationEngine {

	private static final double LOW_BATTERY_THRESHOLD = 25.0;
	private static final double CRITICAL_BATTERY_THRESHOLD = 8.0;
	// Close enough to a waypoint to call it "arrived" and advance to the next one.
	private static final double WAYPOINT_ARRIVAL_RADIUS_M = 15.0;

	private final SimulatorProperties properties;
	private final DroneRegistry registry;
	private final KafkaTemplate<String, TelemetryFrame> kafkaTemplate;
	private final KafkaTemplate<String, MissionEventDto> missionKafkaTemplate;

	@Value("${sarb.kafka.topics.telemetry}")
	private String telemetryTopic;

	@Value("${sarb.kafka.topics.missions}")
	private String missionsTopic;

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
			kafkaTemplate.send(telemetryTopic, drone.getDroneId(), TelemetryFrame.from(drone));
		}
	}

	private void advance(Drone drone, ThreadLocalRandom rnd, double tickSeconds) {
		drone.setLastUpdate(Instant.now());

		if (drone.getStatus() == DroneStatus.LANDED || drone.getStatus() == DroneStatus.IDLE) {
			// Grounded drones don't move; battery stays put.
			return;
		}

		double distanceM = drone.getSpeedMps() * tickSeconds;
		if (drone.getWaypoints() != null && !drone.getWaypoints().isEmpty()) {
			flyMission(drone, distanceM);
		} else {
			wander(drone, rnd, distanceM);
		}

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

	/** Free flight: jittered heading, biased back toward the demo center once it drifts too far. */
	private void wander(Drone drone, ThreadLocalRandom rnd, double distanceM) {
		double jitterDeg = rnd.nextDouble(-8.0, 8.0);
		double headingDeg = GeoMath.normalizeDegrees(drone.getHeadingDeg() + jitterDeg);

		double headingRad = Math.toRadians(headingDeg);
		double metersPerDegreeLon = GeoMath.metersPerDegreeLat() * Math.cos(Math.toRadians(drone.getLat()));

		double newLat = drone.getLat() + (distanceM * Math.cos(headingRad)) / GeoMath.metersPerDegreeLat();
		double newLon = drone.getLon() + (distanceM * Math.sin(headingRad)) / metersPerDegreeLon;

		// Keep drones roughly within the demo area: once beyond the spread
		// radius, steer back toward the center instead of wandering off.
		// (The alert engine still raises GEOFENCE_BREACH on genuine drift -
		// this bias just keeps the simulated fleet visually near Riyadh.)
		double distanceFromCenterKm = GeoMath.haversineKm(properties.centerLat(), properties.centerLon(), newLat,
				newLon);
		if (distanceFromCenterKm > properties.spreadRadiusKm()) {
			headingDeg = GeoMath.normalizeDegrees(GeoMath.bearingDegrees(newLat, newLon, properties.centerLat(),
					properties.centerLon()) + rnd.nextDouble(-10.0, 10.0));
			headingRad = Math.toRadians(headingDeg);
			newLat = drone.getLat() + (distanceM * Math.cos(headingRad)) / GeoMath.metersPerDegreeLat();
			newLon = drone.getLon() + (distanceM * Math.sin(headingRad)) / metersPerDegreeLon;
		}

		drone.setHeadingDeg(headingDeg);
		drone.setLat(newLat);
		drone.setLon(newLon);
	}

	/**
	 * Mission flight: fly straight at the current waypoint (no jitter, no
	 * geofence recentering - the assigned route is authoritative). Once within
	 * {@link #WAYPOINT_ARRIVAL_RADIUS_M} of it, advance to the next one; once
	 * past the last one, publish COMPLETED and hand the drone back to wander().
	 */
	private void flyMission(Drone drone, double distanceM) {
		List<Position> waypoints = drone.getWaypoints();
		Position target = waypoints.get(drone.getWaypointIndex());

		double headingDeg = GeoMath.bearingDegrees(drone.getLat(), drone.getLon(), target.lat(), target.lon());
		double headingRad = Math.toRadians(headingDeg);
		double metersPerDegreeLon = GeoMath.metersPerDegreeLat() * Math.cos(Math.toRadians(drone.getLat()));

		double remainingM = GeoMath.haversineKm(drone.getLat(), drone.getLon(), target.lat(), target.lon()) * 1000.0;

		drone.setHeadingDeg(headingDeg);
		if (remainingM <= Math.max(distanceM, WAYPOINT_ARRIVAL_RADIUS_M)) {
			// Close enough this tick to snap to the waypoint rather than overshoot it.
			drone.setLat(target.lat());
			drone.setLon(target.lon());
			drone.setWaypointIndex(drone.getWaypointIndex() + 1);
			if (drone.getWaypointIndex() >= waypoints.size()) {
				completeMission(drone);
			}
		} else {
			drone.setLat(drone.getLat() + (distanceM * Math.cos(headingRad)) / GeoMath.metersPerDegreeLat());
			drone.setLon(drone.getLon() + (distanceM * Math.sin(headingRad)) / metersPerDegreeLon);
		}
	}

	private void completeMission(Drone drone) {
		String missionId = drone.getMissionId();
		log.info("Drone {} completed mission {}", drone.getDroneId(), missionId);
		drone.setWaypoints(null);
		drone.setWaypointIndex(0);
		drone.setMissionId(null);
		missionKafkaTemplate.send(missionsTopic, drone.getDroneId(),
				new MissionEventDto(Long.valueOf(missionId), drone.getDroneId(), MissionEventType.COMPLETED,
						List.of(), Instant.now()));
	}

	private static double[] randomPointWithinRadius(ThreadLocalRandom rnd, double centerLat, double centerLon,
			double radiusKm) {
		double r = radiusKm * Math.sqrt(rnd.nextDouble());
		double theta = rnd.nextDouble(0, 2 * Math.PI);
		double metersPerDegreeLatKm = GeoMath.metersPerDegreeLat() / 1000.0;
		double deltaLat = (r * Math.cos(theta)) / metersPerDegreeLatKm;
		double deltaLon = (r * Math.sin(theta)) / (metersPerDegreeLatKm * Math.cos(Math.toRadians(centerLat)));
		return new double[] { centerLat + deltaLat, centerLon + deltaLon };
	}
}
