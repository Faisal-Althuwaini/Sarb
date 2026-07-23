package com.dronefleet.alert.model;

import java.time.Instant;

import com.dronefleet.alert.persistence.AlertEntity;

/**
 * Wire shape for both the {@code drone.alerts} Kafka payload and the
 * {@code GET /api/alerts/active} REST response. {@code resolvedAt == null}
 * means the alert is still active.
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
