package com.dronefleet.simulator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topics this service touches so they exist even against a bare
 * Kafka broker. mission.events is "owned" by mission-service (which declares
 * the same spec), but simulator-service also produces to it (STARTED/COMPLETED)
 * so it declares it too - KafkaAdmin no-ops if it already exists.
 */
@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic droneTelemetryTopic(@Value("${sarb.kafka.topics.telemetry}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic missionEventsTopic(@Value("${sarb.kafka.topics.missions}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}
}
