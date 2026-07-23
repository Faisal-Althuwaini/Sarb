package com.dronefleet.telemetry.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.dronefleet.telemetry.model.TelemetryFrame;
import com.dronefleet.telemetry.persistence.FlightLogWriter;

/** Consumes {@code drone.telemetry}: pushes each frame to the frontend and writes a flight log row. */
@RequiredArgsConstructor
@Component
public class TelemetryConsumer {

	private final SimpMessagingTemplate messagingTemplate;
	private final FlightLogWriter flightLogWriter;

	@KafkaListener(
			topics = "${sarb.kafka.topics.telemetry}",
			groupId = "telemetry-service",
			properties = "spring.json.value.default.type=com.dronefleet.telemetry.model.TelemetryFrame")
	public void onTelemetry(TelemetryFrame frame) {
		messagingTemplate.convertAndSend("/topic/telemetry", frame);
		flightLogWriter.record(frame);
	}
}
