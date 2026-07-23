package com.dronefleet.simulator.alert;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One alert episode: open while the rule condition holds, resolved once it clears. */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class AlertEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "drone_id", nullable = false)
	private String droneId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AlertType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AlertSeverity severity;

	@Column(nullable = false)
	private String message;

	@Column(name = "triggered_at", nullable = false)
	private Instant triggeredAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	public AlertEntity(String droneId, AlertType type, AlertSeverity severity, String message, Instant triggeredAt) {
		this.droneId = droneId;
		this.type = type;
		this.severity = severity;
		this.message = message;
		this.triggeredAt = triggeredAt;
	}
}
