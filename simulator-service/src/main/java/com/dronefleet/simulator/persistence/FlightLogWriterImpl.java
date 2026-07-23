package com.dronefleet.simulator.persistence;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dronefleet.simulator.model.Drone;

@RequiredArgsConstructor
@Component
public class FlightLogWriterImpl implements FlightLogWriter {

	private final FlightLogRepository repository;

	@Override
	@Transactional
	public void record(Collection<Drone> drones) {
		List<FlightLogEntity> entities = drones.stream()
				.map(FlightLogEntity::from)
				.toList();
		repository.saveAll(entities);
	}
}
