package com.dronefleet.mission.model;

import java.time.Instant;
import java.util.List;

import com.dronefleet.mission.persistence.MissionEntity;

/** Wire shape for the REST API ({@code GET /api/missions}, {@code POST /api/missions}). */
public record MissionDto(
		Long missionId,
		String droneId,
		MissionStatus status,
		List<WaypointDto> waypoints,
		Instant createdAt,
		Instant startedAt,
		Instant completedAt) {

	public static MissionDto from(MissionEntity entity) {
		return new MissionDto(
				entity.getId(),
				entity.getDroneId(),
				entity.getStatus(),
				entity.getWaypoints().stream().map(w -> w.toDto()).toList(),
				entity.getCreatedAt(),
				entity.getStartedAt(),
				entity.getCompletedAt());
	}
}
