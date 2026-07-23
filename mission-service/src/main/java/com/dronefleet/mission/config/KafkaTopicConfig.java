package com.dronefleet.mission.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declares {@code mission.events} so it exists even against a bare Kafka broker. */
@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic missionEventsTopic(@Value("${sarb.kafka.topics.missions}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}
}
