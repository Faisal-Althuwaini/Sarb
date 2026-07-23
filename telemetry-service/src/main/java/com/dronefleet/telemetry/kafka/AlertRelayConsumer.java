package com.dronefleet.telemetry.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.dronefleet.telemetry.alert.AlertDto;

/** Consumes {@code drone.alerts} and relays it onto the frontend's WebSocket - alert-service owns persistence. */
@RequiredArgsConstructor
@Component
public class AlertRelayConsumer {

	private final SimpMessagingTemplate messagingTemplate;

	@KafkaListener(
			topics = "${sarb.kafka.topics.alerts}",
			groupId = "telemetry-service",
			properties = "spring.json.value.default.type=com.dronefleet.telemetry.alert.AlertDto")
	public void onAlert(AlertDto alert) {
		messagingTemplate.convertAndSend("/topic/alerts", alert);
	}
}
