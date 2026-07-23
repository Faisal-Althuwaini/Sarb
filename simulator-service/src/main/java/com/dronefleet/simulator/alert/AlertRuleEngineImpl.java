package com.dronefleet.simulator.alert;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dronefleet.simulator.geo.Geofence;
import com.dronefleet.simulator.model.Drone;
import com.dronefleet.simulator.model.DroneStatus;

@Slf4j
@RequiredArgsConstructor
@Component
public class AlertRuleEngineImpl implements AlertRuleEngine {

	private final AlertProperties properties;
	private final Geofence geofence;
	private final AlertRepository repository;
	private final SimpMessagingTemplate messagingTemplate;

	// droneId -> active alert per type; mirrors the "resolvedAt IS NULL" rows
	// in Postgres so we don't hit the DB on every rule check, every tick.
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
	public void evaluate(Drone drone) {
		// Grounded drones (landed or never launched) don't get geofence/altitude
		// alerts - only a drone actually flying can breach either rule meaningfully.
		boolean grounded = drone.getStatus() == DroneStatus.LANDED || drone.getStatus() == DroneStatus.IDLE;
		if (!grounded) {
			boolean insideGeofence = geofence.contains(drone.getLat(), drone.getLon());
			evaluateRule(drone, AlertType.GEOFENCE_BREACH, !insideGeofence, AlertSeverity.HIGH,
					() -> "Drone %s left the permitted operating area".formatted(drone.getDroneId()));

			boolean altitudeViolation = drone.getAltitudeM() > properties.maxAltitudeM();
			evaluateRule(drone, AlertType.ALTITUDE_VIOLATION, altitudeViolation, AlertSeverity.HIGH,
					() -> "Drone %s exceeded the maximum altitude of %.0f m".formatted(drone.getDroneId(),
							properties.maxAltitudeM()));
		}

		double battery = drone.getBatteryPct();
		boolean critical = battery <= properties.criticalBatteryPct();
		boolean low = !critical && battery <= properties.lowBatteryPct();

		evaluateRule(drone, AlertType.CRITICAL_BATTERY, critical, AlertSeverity.HIGH,
				() -> "Drone %s battery critical at %.1f%%".formatted(drone.getDroneId(), battery));
		evaluateRule(drone, AlertType.LOW_BATTERY, low, AlertSeverity.MEDIUM,
				() -> "Drone %s battery low at %.1f%%".formatted(drone.getDroneId(), battery));
	}

	private void evaluateRule(Drone drone, AlertType type, boolean triggered, AlertSeverity severity,
			Supplier<String> message) {
		Map<AlertType, AlertEntity> droneAlerts = activeAlerts.computeIfAbsent(drone.getDroneId(),
				id -> new EnumMap<>(AlertType.class));
		AlertEntity active = droneAlerts.get(type);

		if (triggered && active == null) {
			AlertEntity entity = new AlertEntity(drone.getDroneId(), type, severity, message.get(), Instant.now());
			repository.save(entity);
			droneAlerts.put(type, entity);
			messagingTemplate.convertAndSend("/topic/alerts", AlertDto.from(entity));
			log.info("Alert opened: {} ({}) for {}", type, severity, drone.getDroneId());
		} else if (!triggered && active != null) {
			active.setResolvedAt(Instant.now());
			repository.save(active);
			droneAlerts.remove(type);
			messagingTemplate.convertAndSend("/topic/alerts", AlertDto.from(active));
			log.info("Alert resolved: {} for {}", type, drone.getDroneId());
		}
	}
}
