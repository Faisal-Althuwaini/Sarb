package com.dronefleet.alert.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.dronefleet.alert.model.TelemetryFrame;
import com.dronefleet.alert.rule.AlertRuleEngine;

/** Consumes {@code drone.telemetry} and runs it through the rule engine. */
@RequiredArgsConstructor
@Component
public class TelemetryConsumer {

	private final AlertRuleEngine alertRuleEngine;

	@KafkaListener(
			topics = "${sarb.kafka.topics.telemetry}",
			groupId = "alert-service",
			properties = "spring.json.value.default.type=com.dronefleet.alert.model.TelemetryFrame")
	public void onTelemetry(TelemetryFrame frame) {
		alertRuleEngine.evaluate(frame);
	}
}
