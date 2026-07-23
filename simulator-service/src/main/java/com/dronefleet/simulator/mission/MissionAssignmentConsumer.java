package com.dronefleet.simulator.mission;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.dronefleet.simulator.model.Drone;
import com.dronefleet.simulator.model.DroneStatus;
import com.dronefleet.simulator.model.Position;
import com.dronefleet.simulator.registry.DroneRegistry;

/**
 * Consumes {@code mission.events} to learn which drone should fly which
 * route. ASSIGNED switches the drone from random wandering to waypoint
 * following (SimulationEngineImpl.advance) and immediately acks back with
 * STARTED, since this simple model starts flying the instant it's assigned.
 * CANCELLED clears the route so the drone resumes wandering.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MissionAssignmentConsumer {

	private static final double DEFAULT_SPEED_MPS = 10.0;

	private final DroneRegistry registry;
	private final KafkaTemplate<String, MissionEventDto> kafkaTemplate;

	@Value("${sarb.kafka.topics.missions}")
	private String missionsTopic;

	@KafkaListener(
			topics = "${sarb.kafka.topics.missions}",
			groupId = "simulator-service",
			properties = "spring.json.value.default.type=com.dronefleet.simulator.mission.MissionEventDto")
	public void onMissionEvent(MissionEventDto event) {
		registry.get(event.droneId()).ifPresentOrElse(drone -> {
			switch (event.type()) {
				case ASSIGNED -> assign(drone, event);
				case CANCELLED -> cancel(drone, event);
				default -> {
					// STARTED/COMPLETED are published by this service, not consumed by it.
				}
			}
		}, () -> log.warn("Mission event for unknown drone {}", event.droneId()));
	}

	private void assign(Drone drone, MissionEventDto event) {
		List<Position> route = event.waypoints().stream().map(w -> new Position(w.lat(), w.lon())).toList();
		drone.setWaypoints(route);
		drone.setWaypointIndex(0);
		drone.setMissionId(String.valueOf(event.missionId()));
		drone.setStatus(DroneStatus.IN_FLIGHT);
		if (drone.getSpeedMps() <= 0) {
			drone.setSpeedMps(DEFAULT_SPEED_MPS);
		}
		log.info("Drone {} assigned mission {} with {} waypoints", drone.getDroneId(), event.missionId(),
				route.size());

		kafkaTemplate.send(missionsTopic, drone.getDroneId(),
				new MissionEventDto(event.missionId(), drone.getDroneId(), MissionEventType.STARTED, List.of(),
						Instant.now()));
	}

	private void cancel(Drone drone, MissionEventDto event) {
		drone.setWaypoints(null);
		drone.setWaypointIndex(0);
		drone.setMissionId(null);
		log.info("Drone {} mission {} cancelled - resuming free flight", drone.getDroneId(), event.missionId());
	}
}
