package com.dronefleet.simulator.mission;

import java.time.Instant;
import java.util.List;

/** Wire shape for {@code mission.events}. See mission-service's copy for the canonical contract. */
public record MissionEventDto(
		Long missionId,
		String droneId,
		MissionEventType type,
		List<WaypointDto> waypoints,
		Instant timestamp) {
}
