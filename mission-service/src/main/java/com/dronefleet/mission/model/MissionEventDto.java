package com.dronefleet.mission.model;

import java.time.Instant;
import java.util.List;

/**
 * Wire shape for {@code mission.events}. {@code waypoints} is only populated
 * on ASSIGNED (the payload simulator-service needs to start flying the
 * route); STARTED/COMPLETED/CANCELLED carry an empty list.
 */
public record MissionEventDto(
		Long missionId,
		String droneId,
		MissionEventType type,
		List<WaypointDto> waypoints,
		Instant timestamp) {
}
