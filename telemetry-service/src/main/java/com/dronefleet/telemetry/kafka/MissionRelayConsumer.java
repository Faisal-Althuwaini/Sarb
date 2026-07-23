package com.dronefleet.telemetry.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.dronefleet.telemetry.mission.MissionEventDto;

/** Consumes {@code mission.events} and relays it onto the frontend's WebSocket - mission-service owns persistence. */
@RequiredArgsConstructor
@Component
public class MissionRelayConsumer {

	private final SimpMessagingTemplate messagingTemplate;

	@KafkaListener(
			topics = "${sarb.kafka.topics.missions}",
			groupId = "telemetry-service",
			properties = "spring.json.value.default.type=com.dronefleet.telemetry.mission.MissionEventDto")
	public void onMissionEvent(MissionEventDto event) {
		messagingTemplate.convertAndSend("/topic/missions", event);
	}
}
