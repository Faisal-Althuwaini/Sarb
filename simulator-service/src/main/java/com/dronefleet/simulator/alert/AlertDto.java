package com.dronefleet.simulator.alert;

import java.time.Instant;

/**
 * Wire shape broadcast on {@code /topic/alerts} and returned by the active-alerts
 * REST endpoint. {@code resolvedAt == null} means the alert is still active.
 */
public record AlertDto(
		Long alertId,
		String droneId,
		AlertType type,
		AlertSeverity severity,
		String message,
		Instant triggeredAt,
		Instant resolvedAt) {

	public static AlertDto from(AlertEntity entity) {
		return new AlertDto(
				entity.getId(),
				entity.getDroneId(),
				entity.getType(),
				entity.getSeverity(),
				entity.getMessage(),
				entity.getTriggeredAt(),
				entity.getResolvedAt());
	}
}
