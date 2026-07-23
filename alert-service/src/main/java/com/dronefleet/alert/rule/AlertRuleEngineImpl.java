package com.dronefleet.alert.rule;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dronefleet.alert.config.AlertProperties;
import com.dronefleet.alert.geo.Geofence;
import com.dronefleet.alert.model.AlertDto;
import com.dronefleet.alert.model.AlertSeverity;
import com.dronefleet.alert.model.AlertType;
import com.dronefleet.alert.model.DroneStatus;
import com.dronefleet.alert.model.TelemetryFrame;
import com.dronefleet.alert.persistence.AlertEntity;
import com.dronefleet.alert.persistence.AlertRepository;

@Slf4j
@RequiredArgsConstructor
@Component
public class AlertRuleEngineImpl implements AlertRuleEngine {

	private final AlertProperties properties;
	private final Geofence geofence;
	private final AlertRepository repository;
	private final KafkaTemplate<String, AlertDto> kafkaTemplate;

	@Value("${sarb.kafka.topics.alerts}")
	private String alertsTopic;

	// droneId -> active alert per type; mirrors the "resolvedAt IS NULL" rows
	// in Postgres so we don't hit the DB on every rule check, every message.
	private final Map<String, Map<AlertType, AlertEntity>> activeAlerts = new ConcurrentHashMap<>();

	@PostConstruct
	void loadActiveAlerts() {
		for (AlertEntity alert : repository.findByResolvedAtIsNullOrderByTriggeredAtDesc()) {
			activeAlerts.computeIfAbsent(alert.getDroneId(), id -> new EnumMap<>(AlertType.class))
					.put(alert.getType(), alert);
		}
	}

	@Override
	@Transactional
	public void evaluate(TelemetryFrame frame) {
		// Grounded drones (landed or never launched) don't get geofence/altitude
		// alerts - only a drone actually flying can breach either rule meaningfully.
		boolean grounded = frame.status() == DroneStatus.LANDED || frame.status() == DroneStatus.IDLE;
		if (!grounded) {
			boolean insideGeofence = geofence.contains(frame.position().lat(), frame.position().lon());
			evaluateRule(frame, AlertType.GEOFENCE_BREACH, !insideGeofence, AlertSeverity.HIGH,
					() -> "Drone %s left the permitted operating area".formatted(frame.droneId()));

			boolean altitudeViolation = frame.altitudeM() > properties.maxAltitudeM();
			evaluateRule(frame, AlertType.ALTITUDE_VIOLATION, altitudeViolation, AlertSeverity.HIGH,
					() -> "Drone %s exceeded the maximum altitude of %.0f m".formatted(frame.droneId(),
							properties.maxAltitudeM()));
		}

		double battery = frame.batteryPct();
		boolean critical = battery <= properties.criticalBatteryPct();
		boolean low = !critical && battery <= properties.lowBatteryPct();

		evaluateRule(frame, AlertType.CRITICAL_BATTERY, critical, AlertSeverity.HIGH,
				() -> "Drone %s battery critical at %.1f%%".formatted(frame.droneId(), battery));
		evaluateRule(frame, AlertType.LOW_BATTERY, low, AlertSeverity.MEDIUM,
				() -> "Drone %s battery low at %.1f%%".formatted(frame.droneId(), battery));
	}

	private void evaluateRule(TelemetryFrame frame, AlertType type, boolean triggered, AlertSeverity severity,
			Supplier<String> message) {
		Map<AlertType, AlertEntity> droneAlerts = activeAlerts.computeIfAbsent(frame.droneId(),
				id -> new EnumMap<>(AlertType.class));
		AlertEntity active = droneAlerts.get(type);

		if (triggered && active == null) {
			AlertEntity entity = new AlertEntity(frame.droneId(), type, severity, message.get(), Instant.now());
			repository.save(entity);
			droneAlerts.put(type, entity);
			kafkaTemplate.send(alertsTopic, frame.droneId(), AlertDto.from(entity));
			log.info("Alert opened: {} ({}) for {}", type, severity, frame.droneId());
		} else if (!triggered && active != null) {
			active.setResolvedAt(Instant.now());
			repository.save(active);
			droneAlerts.remove(type);
			kafkaTemplate.send(alertsTopic, frame.droneId(), AlertDto.from(active));
			log.info("Alert resolved: {} for {}", type, frame.droneId());
		}
	}
}
