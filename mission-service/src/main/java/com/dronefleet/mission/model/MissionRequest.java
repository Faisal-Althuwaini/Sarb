package com.dronefleet.mission.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** {@code POST /api/missions} request body: assign a route to a drone. */
public record MissionRequest(
		@NotBlank String droneId,
		@NotEmpty List<WaypointDto> waypoints) {
}
