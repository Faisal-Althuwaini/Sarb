package com.dronefleet.mission.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.dronefleet.mission.model.MissionEventDto;
import com.dronefleet.mission.service.MissionService;

/**
 * Consumes mission-service's own {@code mission.events} topic to pick up the
 * STARTED/COMPLETED events simulator-service publishes back - it's the only
 * party that knows when a drone actually begins/finishes flying a route.
 */
@RequiredArgsConstructor
@Component
public class MissionProgressConsumer {

	private final MissionService missionService;

	@KafkaListener(
			topics = "${sarb.kafka.topics.missions}",
			groupId = "mission-service",
			properties = "spring.json.value.default.type=com.dronefleet.mission.model.MissionEventDto")
	public void onMissionEvent(MissionEventDto event) {
		missionService.applyProgress(event);
	}
}
