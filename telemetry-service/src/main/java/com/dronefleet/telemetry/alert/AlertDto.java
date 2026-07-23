package com.dronefleet.telemetry.alert;

import java.time.Instant;

/**
 * Mirrors alert-service's {@code AlertDto} - the {@code drone.alerts} Kafka
 * payload. telemetry-service only relays this onto {@code /topic/alerts};
 * alert-service owns persistence.
 */
public record AlertDto(
		Long alertId,
		String droneId,
		AlertType type,
		AlertSeverity severity,
		String message,
		Instant triggeredAt,
		Instant resolvedAt) {
}
